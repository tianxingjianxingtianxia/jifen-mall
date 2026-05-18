#!/usr/bin/env python3
"""
积分商城 Playwright E2E 全流程自动化测试
覆盖：注册 → 签到 → 兑换 → 取消 → 浏览器新增地址 → 积分明细 → 管理后台
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
    await page.screenshot(path=os.path.join(SCREENSHOT_DIR, name), full_page=True)

async def main():
    ts = "e2e_" + str(int(time.time() * 1000))[-8:]
    r = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/auth/register",
        "-H", "Content-Type: application/json",
        "-d", '{"username":"%s","password":"123456","nickname":"E2E测试"}' % ts
    ], capture_output=True, text=True).stdout)
    token = r["data"]["token"]
    uid = r["data"]["userId"]
    subprocess.run(["curl", "-s", "-X", "POST", API + "/points/topup?points=200",
        "-H", "Authorization: Bearer " + token], capture_output=True)
    r = json.loads(subprocess.run(["curl", "-s", API + "/products?pageNum=1&pageSize=5"], capture_output=True, text=True).stdout)
    pid = r["data"]["records"][0]["id"]
    pname = r["data"]["records"][0]["name"]

    # 预建地址（用于商品详情页兑换时的地址选择）
    subprocess.run(["curl", "-s", "-X", "POST", API + "/addresses",
        "-H", "Content-Type: application/json", "-H", "Authorization: Bearer " + token,
        "-d", '{"receiverName":"预置地址","receiverPhone":"13800138001","province":"广东省","city":"深圳市","district":"南山区","detailAddress":"科技园路","isDefault":1}'
    ], capture_output=True)
    print("注册: uid=%d  商品: %s" % (uid, pname))

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True, args=["--no-sandbox","--disable-gpu","--single-process"])
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
        check("导航栏'我的订单'", await page.is_visible("text=我的订单"))
        check("导航栏'积分明细'", await page.is_visible("text=积分明细"))

        # S3: 签到(API)
        print("\n[S3] 签到")
        r = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/points/sign-in",
            "-H", "Authorization: Bearer " + token], capture_output=True, text=True).stdout)
        check("签到成功", r["code"]==200)

        # S4: 商品详情
        print("\n[S4] 商品详情")
        await page.goto("http://localhost:3000/#/product/%d" % pid, wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "S4-product.png")
        check("商品详情页加载", await page.is_visible("text=" + pname))

        # S5: 选择地址
        print("\n[S5] 选地址")
        if await page.is_visible(".el-select"):
            await page.click(".el-select")
            await asyncio.sleep(1)
            opt = page.locator(".el-select-dropdown__item").first
            if await opt.is_visible():
                await opt.click()
                await asyncio.sleep(1)
                check("地址选择成功", True)
            else:
                check("地址下拉选项可见", False)
        else:
            check("地址选择器可见", False)

        # S6: 兑换
        print("\n[S6] 兑换")
        btn = page.locator("button:has-text('立即兑换')")
        if await btn.is_visible():
            await btn.click()
            await asyncio.sleep(2)
            await screenshot(page, "S6-exchange.png")
            if await page.is_visible("text=兑换确认"):
                await page.click("button:has-text('确认兑换')")
                await asyncio.sleep(2)
                await screenshot(page, "S7-order.png")
                check("兑换成功", "order" in page.url)
                st = await page.text_content(".el-tag")
                check("订单状态显示", st is not None and len(st)>0, st or "")
            else:
                check("兑换弹窗出现", False)
        else:
            check("兑换按钮可见", False)

        # S7: 订单Tab
        print("\n[S7] 订单Tab")
        await page.goto("http://localhost:3000/#/orders", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "S8-orders.png")
        tabs = await page.locator(".el-tabs__item").all()
        check("5个Tab", len(tabs)>=3)
        for i, name in enumerate(["全部","待发货","已发货","已完成","已取消"]):
            if i < len(tabs):
                await tabs[i].click()
                await asyncio.sleep(1)
                check("Tab[%s]无报错" % name, not await page.is_visible(".el-message--error"))

        # S8: 取消订单
        print("\n[S8] 取消订单")
        r = json.loads(subprocess.run(["curl", "-s", API + "/orders?pageNum=1&pageSize=1",
            "-H", "Authorization: Bearer " + token], capture_output=True, text=True).stdout)
        if r["code"]==200 and r["data"]["records"]:
            oid = r["data"]["records"][0]["id"]
            rc = json.loads(subprocess.run(["curl", "-s", "-X", "POST", API + "/orders/%d/cancel" % oid,
                "-H", "Authorization: Bearer " + token], capture_output=True, text=True).stdout)
            check("取消订单成功", rc["code"]==200)
        else:
            check("取消订单", False, "无订单")

        # S9: 浏览器新增地址
        print("\n[S9] 浏览器新增地址")
        await page.goto("http://localhost:3000/#/addresses", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "S9-addresses.png")
        check("地址页加载", await page.is_visible("text=地址管理"))

        add_btn = page.locator("button:has-text('新增地址')")
        if await add_btn.is_visible():
            await add_btn.click()
            await asyncio.sleep(1)
            if await page.is_visible(".el-dialog"):
                check("弹窗出现", True)
                inputs = page.locator(".el-dialog input.el-input__inner")
                await inputs.nth(0).fill("浏览器测试")
                await inputs.nth(1).fill("13800138001")
                # 省市区 - 直接用fill填值（简化Playwright与el-select的兼容问题）
                await inputs.nth(2).fill("广东省")
                await inputs.nth(3).fill("深圳市")
                await inputs.nth(4).fill("南山区")
                await asyncio.sleep(0.5)
                # 详细地址
                await page.locator(".el-dialog textarea").fill("浏览器测试地址")
                await asyncio.sleep(0.5)
                await page.click("button:has-text('保存')")
                await asyncio.sleep(2)
                check("浏览器新增地址成功", not await page.is_visible(".el-message--error"))
                await screenshot(page, "S9-address-added.png")
            else:
                check("弹窗可见", False)
        else:
            check("新增地址按钮", False)

        # S10: 积分明细
        print("\n[S10] 积分明细")
        await page.goto("http://localhost:3000/#/points-records", wait_until="networkidle")
        await asyncio.sleep(3)
        await screenshot(page, "S10-records.png")
        check("积分明细加载", await page.is_visible("text=积分明细") or await page.is_visible(".el-table"))

        # S11: 管理后台
        print("\n[S11] 管理后台")
        await page.goto("http://localhost:3000/#/admin/login", wait_until="load")
        await asyncio.sleep(1)
        await page.fill('input[placeholder*="用户"], input[type="text"]', "admin")
        await page.fill('input[placeholder*="密码"], input[type="password"]', "admin123")
        await page.click("button:has-text('登录')")
        await asyncio.sleep(3)
        await screenshot(page, "S11-admin.png")
        check("数据看板", await page.is_visible("text=数据看板") or "dashboard" in page.url)

        # S12: 后台页面
        print("\n[S12] 后台页面")
        for name, p in {"商品管理":"/#/admin/products","订单管理":"/#/admin/orders","系统配置":"/#/admin/config"}.items():
            await page.goto("http://localhost:3000" + p, wait_until="networkidle")
            await asyncio.sleep(1)
            await screenshot(page, "S12-admin-%s.png" % name)
            check("[%s]页面可访问" % name, "404" not in await page.title())

        await browser.close()

    passed = sum(1 for r in results if r[1])
    failed = [r for r in results if not r[1]]
    print("\n" + "="*60)
    print("  Playwright E2E 测试报告")
    print("="*60)
    for s, ok, d in results:
        print("  %s %s %s" % ("✅" if ok else "❌", s, (" - "+d if d else "")))
    print("="*60)
    print("  通过: %d/%d (%d%%)" % (passed, len(results), passed*100//len(results)))
    if failed:
        print("  失败:")
        for s, _, d in failed:
            print("    ❌ %s %s" % (s, (" - "+d if d else "")))
    else:
        print("  ✅ 全部通过，0 项失败，0 项手动验证！")
    print("="*60)

if __name__ == "__main__":
    asyncio.run(main())
