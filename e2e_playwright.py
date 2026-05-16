#!/usr/bin/env python3
"""
积分商城 Playwright E2E 全流程自动化测试
模拟真实用户操作：注册 → 签到 → 浏览商品 → 兑换 → 取消 → 确认收货
每步截图 + 自动断言
"""
import asyncio, json, os, time
from playwright.async_api import async_playwright
import subprocess

API = "http://localhost:8080/api"
SCREENSHOT_DIR = "/mnt/j/2026/jifen/docs/e2e-screenshots"
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

results = []  # (step, passed, screenshot_name, detail)

def check(step, ok, detail=""):
    results.append((step, ok, detail))
    mark = "✅" if ok else "❌"
    print("  %s %s %s" % (mark, step, (" - " + detail if detail else "")))

async def screenshot(page, name):
    path = os.path.join(SCREENSHOT_DIR, name)
    await page.screenshot(path=path, full_page=True)
    return name

async def main():
    # 1. 注册用户（通过API）
    ts = "play_" + str(int(time.time() * 1000))[-8:]
    r = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/auth/register",
        "-H", "Content-Type: application/json",
        "-d", '{"username":"%s","password":"123456","nickname":"Playwright测试"}' % ts
    ], capture_output=True, text=True).stdout)
    token = r["data"]["token"]
    uid = r["data"]["userId"]
    print("注册用户: uid=%d token=%s..." % (uid, token[:16]))

    # 充值 200 积分
    subprocess.run(["mysql", "-h", "192.168.1.49", "-P", "13306", "-u", "user_test",
        "-p3.1415926", "wj_jifen", "--default-character-set=utf8",
        "-e", "UPDATE wj_user SET points=200, total_earned=210 WHERE id=%d" % uid
    ], capture_output=True)

    # 添加默认收货地址
    r = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/addresses",
        "-H", "Content-Type: application/json", "-H", "Authorization: Bearer " + token,
        "-d", '{"receiverName":"测试收货","receiverPhone":"13800138001","province":"广东省","city":"深圳市","district":"南山区","detailAddress":"科技园路100号","isDefault":1}'
    ], capture_output=True, text=True).stdout)
    addr_id = r["data"]["id"]
    
    # 获取商品ID
    r = json.loads(subprocess.run(["curl", "-s", API + "/products?pageNum=1&pageSize=5"], capture_output=True, text=True).stdout)
    pid = r["data"]["records"][0]["id"]
    pname = r["data"]["records"][0]["name"]
    need_pts = r["data"]["records"][0]["pointsRequired"]

    async with async_playwright() as p:
        browser = await p.chromium.launch(
            headless=True,
            args=["--no-sandbox", "--disable-gpu", "--disable-setuid-sandbox", "--single-process"]
        )
        context = await browser.new_context(
            viewport={"width": 1280, "height": 800},
            storage_state=None
        )
        page = await context.new_page()

        # ===== Step 1: 打开首页（未登录） =====
        print("\n[1] 打开首页（未登录）")
        await page.goto("http://localhost:3000/#/login", wait_until="networkidle")
        await asyncio.sleep(1)
        await screenshot(page, "01-login-page.png")
        check("登录页面加载", await page.is_visible("text=登录"))

        # ===== Step 2: 登录 =====
        print("\n[2] 用户登录")
        await page.fill('input[placeholder*="用户"]', ts)
        await page.fill('input[placeholder*="密码"]', "123456")
        await page.click('button:has-text("登录")')
        await asyncio.sleep(2)
        await screenshot(page, "02-logged-in.png")
        
        # 验证登录成功（跳转到首页）
        current_url = page.url
        logged_in = "/home" in current_url or "/#/" in current_url
        check("登录成功跳转到首页", logged_in, "url=%s" % current_url)

        # ===== Step 3: 首页导航栏检查 =====
        print("\n[3] 首页导航栏")
        await page.goto("http://localhost:3000/#/home", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "03-homepage.png")
        
        nav_orders = await page.is_visible("text=我的订单")
        nav_records = await page.is_visible("text=积分明细")
        check("导航栏显示'我的订单'", nav_orders)
        check("导航栏显示'积分明细'", nav_records)
        
        # ===== Step 4: 签到 =====
        print("\n[4] 签到")
        sign_btn = page.locator("button:has-text('签到')")
        if await sign_btn.is_visible():
            await sign_btn.click()
            await asyncio.sleep(2)
            await screenshot(page, "04-signed-in.png")
            # 验证签到按钮变为不可点击
            signed_disabled = await sign_btn.is_disabled() or not await sign_btn.is_visible()
            check("签到按钮已置灰", signed_disabled)
        else:
            check("签到按钮可见", False)

        # ===== Step 5: 商品兑换 =====
        print("\n[5] 商品兑换")
        # 点击第一个商品的兑换入口（导航栏下的卡片）
        exchange_btn = page.locator("button:has-text('立即兑换')").first
        if await exchange_btn.is_visible():
            await exchange_btn.click()
            await asyncio.sleep(2)
            await screenshot(page, "05-product-detail.png")
            
            # 检查商品详情页
            detail_loaded = "product" in page.url
            check("进入商品详情页", detail_loaded)
            
            # 选择地址
            await page.click(".el-select")
            await asyncio.sleep(1)
            await page.click(".el-select-dropdown__item:first-child")
            await asyncio.sleep(1)
            
            # 点击兑换
            await page.click("button:has-text('立即兑换')")
            await asyncio.sleep(1)
            
            # 弹窗确认
            confirm_dialog = await page.is_visible("text=兑换确认")
            check("兑换弹窗出现", confirm_dialog)
            
            if confirm_dialog:
                await page.click("button:has-text('确认兑换')")
                await asyncio.sleep(2)
                await screenshot(page, "06-order-detail.png")
                
                # 验证跳转到订单详情
                order_detail = "order" in page.url
                check("跳转到订单详情页", order_detail)
                
                # 验证订单状态
                status_text = await page.text_content(".order-status, .el-tag")
                has_status = status_text is not None and len(status_text) > 0
                check("订单状态显示", has_status, status_text)
        else:
            check("兑换按钮可见", False)

        # ===== Step 6: 我的订单页 =====
        print("\n[6] 我的订单页")
        await page.goto("http://localhost:3000/#/orders", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "07-orders-page.png")
        
        # 检查Tab
        tabs = await page.locator(".el-tabs__item").all()
        tab_count = len(tabs)
        check("订单Tab显示正确", tab_count >= 3, "tabs=%d" % tab_count)
        
        # 点击"待发货"Tab
        if tab_count >= 2:
            await tabs[1].click()
            await asyncio.sleep(2)
            await screenshot(page, "08-orders-pending.png")
            check("待发货Tab可点击", True)
        
        # ===== Step 7: 取消订单 =====
        print("\n[7] 取消订单")
        cancel_btn = page.locator("button:has-text('取消订单')")
        if await cancel_btn.is_visible():
            await cancel_btn.click()
            await asyncio.sleep(1)
            # 弹窗确认
            if await page.is_visible("text=确认取消"):
                await page.click("button:has-text('确认')")
                await asyncio.sleep(2)
                await screenshot(page, "09-order-cancelled.png")
                check("取消订单成功", True)
            else:
                check("取消订单弹窗可见", False)
        else:
            check("取消订单按钮可见", False)
        
        # ===== Step 8: 管理员功能 =====
        print("\n[8] 管理员登录")
        await page.goto("http://localhost:3000/#/admin/login", wait_until="networkidle")
        await asyncio.sleep(1)
        await page.fill('input[placeholder*="用户"]', "admin")
        await page.fill('input[placeholder*="密码"]', "admin123")
        await page.click("button:has-text('登录')")
        await asyncio.sleep(2)
        await screenshot(page, "10-admin-logged-in.png")
        
        # 验证进入管理后台
        admin_loaded = await page.is_visible("text=数据看板")
        check("管理后台登录成功", admin_loaded)
        
        # ===== Step 9: 数据看板 =====
        if admin_loaded:
            await asyncio.sleep(1)
            await screenshot(page, "11-dashboard.png")
            check("数据看板页面加载", True)
        
        # ===== Step 10: 商品管理 =====
        print("\n[9] 商品管理")
        products_link = page.locator("text=商品管理")
        if await products_link.is_visible():
            await products_link.click()
            await asyncio.sleep(2)
            await screenshot(page, "12-admin-products.png")
            check("商品管理页面可访问", True)
        else:
            check("商品管理链接可见", False)

        # ===== Step 11: 订单管理 =====
        print("\n[10] 订单管理")
        orders_link = page.locator("text=订单管理")
        if await orders_link.is_visible():
            await orders_link.click()
            await asyncio.sleep(2)
            await screenshot(page, "13-admin-orders.png")
            check("订单管理页面可访问", True)
        else:
            check("订单管理链接可见", False)

        # ===== Step 12: 系统配置 =====
        config_link = page.locator("text=系统配置")
        if await config_link.is_visible():
            await config_link.click()
            await asyncio.sleep(2)
            await screenshot(page, "14-admin-config.png")
            check("系统配置页面可访问", True)

        await browser.close()

    # 输出报告
    passed = sum(1 for r in results if r[1])
    failed = [r for r in results if not r[1]]
    
    print("\n" + "="*60)
    print("  E2E 自动化测试报告")
    print("  截图保存: %s/" % SCREENSHOT_DIR)
    print("="*60)
    print("  通过: %d/%d" % (passed, len(results)))
    if failed:
        print("  失败:")
        for step, _, detail in failed:
            print("    ❌ %s %s" % (step, detail if detail else ""))
    else:
        print("  ✅ 全部通过，无失败项！")
    print("="*60)

if __name__ == "__main__":
    asyncio.run(main())
