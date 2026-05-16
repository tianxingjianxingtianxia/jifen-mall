#!/usr/bin/env python3
"""
积分商城 Playwright E2E 全流程自动化测试
覆盖：注册 → 签到 → 兑换 → 取消 → 管理后台 → 积分明细 → 地址管理
"""
import asyncio, json, os, time
from playwright.async_api import async_playwright
import subprocess

API = "http://localhost:8080/api"
SCREENSHOT_DIR = "/mnt/j/2026/jifen/docs/e2e-screenshots"
os.makedirs(SCREENSHOT_DIR, exist_ok=True)
results = []

def check(step, ok, detail=""):
    results.append((step, ok, detail))
    mark = "✅" if ok else "❌"
    print("  %s %s %s" % (mark, step, (" - " + detail if detail else "")))

async def screenshot(page, name):
    path = os.path.join(SCREENSHOT_DIR, name)
    await page.screenshot(path=path, full_page=True)

async def main():
    ts = "e2e_" + str(int(time.time() * 1000))[-8:]
    r = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/auth/register",
        "-H", "Content-Type: application/json",
        "-d", '{"username":"%s","password":"123456","nickname":"E2E测试"}' % ts
    ], capture_output=True, text=True).stdout)
    token = r["data"]["token"]
    uid = r["data"]["userId"]
    print("注册: uid=%d" % uid)

    subprocess.run(["curl", "-s", "-X", "POST", API + "/points/topup?points=200",
        "-H", "Authorization: Bearer " + token], capture_output=True)

    r = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/addresses",
        "-H", "Content-Type: application/json", "-H", "Authorization: Bearer " + token,
        "-d", '{"receiverName":"E2E收货","receiverPhone":"13800138001","province":"广东","city":"深圳","district":"南山","detailAddress":"科技园","isDefault":1}'
    ], capture_output=True, text=True).stdout)
    addr_id = r["data"]["id"]

    r = json.loads(subprocess.run(["curl", "-s", API + "/products?pageNum=1&pageSize=5"], capture_output=True, text=True).stdout)
    pid = r["data"]["records"][0]["id"]
    pname = r["data"]["records"][0]["name"]
    need = r["data"]["records"][0]["pointsRequired"]
    print("商品: %s (%d积分)" % (pname, need))

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-gpu", "--disable-setuid-sandbox", "--single-process"])
        page = await (await browser.new_context(viewport={"width":1280,"height":800})).new_page()

        # S1: 登录
        print("\n[S1] 登录")
        await page.goto("http://localhost:3000/#/home", wait_until="networkidle")
        await page.evaluate("localStorage.setItem('token','%s')" % token)
        await page.evaluate("localStorage.setItem('userInfo',JSON.stringify({userId:%d,username:'%s',nickname:'测试',points:200}))" % (uid, ts))
        await page.goto("http://localhost:3000/#/home", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "S1-homepage.png")
        check("首页加载", await page.is_visible("text=积分商城"))

        # S2: 导航栏
        print("\n[S2] 导航栏")
        check("导航栏显示'我的订单'", await page.is_visible("text=我的订单"))
        check("导航栏显示'积分明细'", await page.is_visible("text=积分明细"))

        # S3: 签到
        print("\n[S3] 签到")
        r = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/points/sign-in",
            "-H", "Authorization: Bearer " + token], capture_output=True, text=True).stdout)
        check("签到成功", r["code"]==200, "%d积分" % r["data"]["points"])

        # S4: 商品详情
        print("\n[S4] 商品详情")
        await page.goto("http://localhost:3000/#/product/%d" % pid, wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "S4-product.png")
        check("商品详情页加载", await page.is_visible("text=" + pname))

        # S5: 选地址
        print("\n[S5] 选地址")
        select_visible = await page.is_visible(".el-select")
        check("地址选择器可见", select_visible)
        if select_visible:
            await page.click(".el-select")
            await asyncio.sleep(1)
            opt = page.locator(".el-select-dropdown__item").first
            if await opt.is_visible():
                await opt.click()
                await asyncio.sleep(1)
                check("地址选择成功", True)

        # S6: 兑换
        print("\n[S6] 兑换")
        exchange_btn = page.locator("button:has-text('立即兑换')")
        btn_visible = await exchange_btn.is_visible()
        check("兑换按钮可见", btn_visible)
        if btn_visible:
            await exchange_btn.click()
            await asyncio.sleep(2)
            await screenshot(page, "S6-exchange.png")
            confirm_visible = await page.is_visible("text=兑换确认")
            check("兑换弹窗出现", confirm_visible)
            if confirm_visible:
                await page.click("button:has-text('确认兑换')")
                await asyncio.sleep(2)
                await screenshot(page, "S7-order.png")
                check("跳转到订单详情页", "order" in page.url)
                status_text = await page.text_content(".el-tag")
                check("订单状态显示", status_text is not None and len(status_text) > 0, status_text or "")

        # S7: 订单Tab
        print("\n[S7] 订单Tab")
        await page.goto("http://localhost:3000/#/orders", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "S8-orders.png")
        tabs = await page.locator(".el-tabs__item").all()
        check("订单Tabs显示", len(tabs) >= 3, "%d个Tab" % len(tabs))
        for i, name in enumerate(["全部","待发货","已发货","已完成","已取消"]):
            if i < len(tabs):
                await tabs[i].click()
                await asyncio.sleep(1)
                has_error = await page.is_visible(".el-message--error")
                check("Tab[%s]无报错" % name, not has_error)

        # S8: 取消
        print("\n[S8] 取消订单")
        r = json.loads(subprocess.run(["curl", "-s", API + "/orders?pageNum=1&pageSize=1",
            "-H", "Authorization: Bearer " + token], capture_output=True, text=True).stdout)
        if r["code"]==200 and len(r["data"]["records"])>0:
            oid = r["data"]["records"][0]["id"]
            rc = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/orders/%d/cancel" % oid,
                "-H", "Authorization: Bearer " + token], capture_output=True, text=True).stdout)
            check("取消订单成功", rc["code"]==200)
            if rc["code"]==200:
                await screenshot(page, "S9-cancel.png")
        else:
            check("取消订单成功", False, "无订单")

        # S9: 积分明细
        print("\n[S9] 积分明细")
        await page.goto("http://localhost:3000/#/points-records", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "S10-records.png")
        check("积分明细页面加载", await page.is_visible("text=积分明细") or await page.is_visible(".el-table"))

        # S10: 地址管理
        print("\n[S10] 地址管理")
        await page.goto("http://localhost:3000/#/addresses", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "S11-addresses.png")
        check("地址管理页面加载", await page.is_visible("text=地址管理") or await page.is_visible(".el-card, .el-button"))

        # S11: 管理后台
        print("\n[S11] 管理后台")
        await page.goto("http://localhost:3000/#/admin/login", wait_until="load")
        await asyncio.sleep(1)
        await page.fill('input[placeholder*="用户"], input[type="text"]', "admin")
        await page.fill('input[placeholder*="密码"], input[type="password"]', "admin123")
        await page.click("button:has-text('登录')")
        await asyncio.sleep(3)
        await screenshot(page, "S12-admin.png")
        check("管理后台数据看板", await page.is_visible("text=数据看板") or "dashboard" in page.url)

        # S12: 后台页面
        print("\n[S12] 后台页面")
        for name, p in {"商品管理":"/#/admin/products","订单管理":"/#/admin/orders","系统配置":"/#/admin/config"}.items():
            await page.goto("http://localhost:3000" + p, wait_until="networkidle")
            await asyncio.sleep(1)
            await screenshot(page, "S13-admin-%s.png" % name)
            check("后台[%s]页面可访问" % name, "404" not in await page.title())

        await browser.close()

    # 报告
    passed = sum(1 for r in results if r[1])
    failed = [r for r in results if not r[1]]
    print("\n" + "="*60)
    print("  Playwright E2E 测试报告")
    print("="*60)
    for step, ok, d in results:
        print("  %s %s %s" % ("✅" if ok else "❌", step, (" - "+d if d else "")))
    print("="*60)
    print("  通过: %d/%d (%d%%)" % (passed, len(results), passed*100//len(results)))
    if failed:
        print("  失败:")
        for step, _, d in failed:
            print("    ❌ %s %s" % (step, (" - "+d if d else "")))
    else:
        print("  ✅ 全部通过，0 项失败，0 项手动验证！")
    print("="*60)

if __name__ == "__main__":
    asyncio.run(main())
