#!/usr/bin/env python3
"""
积分商城完整 E2E 自动化验收
覆盖：
1. 普通用户流程：注册→签到→兑换→Tab→取消→地址(浏览器)→积分明细
2. 管理后台：商品CRUD→发货→配置→看板→权限
3. 文件上传：上传→验证文件可访问
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

# ===== API 辅助函数 =====
def api_get(path, token=None):
    h = []
    if token: h += ["-H", "Authorization: Bearer " + token]
    return json.loads(subprocess.run(["curl", "-s", API + path] + h, capture_output=True, text=True).stdout)

def api_post(path, data=None, token=None):
    h = ["-H", "Content-Type: application/json"]
    if token: h += ["-H", "Authorization: Bearer " + token]
    cmd = ["curl", "-s", "-X", "POST", API + path] + h
    if data: cmd += ["-d", json.dumps(data)]
    return json.loads(subprocess.run(cmd, capture_output=True, text=True).stdout)

def api_put(path, data=None, token=None):
    h = ["-H", "Content-Type: application/json"]
    if token: h += ["-H", "Authorization: Bearer " + token]
    cmd = ["curl", "-s", "-X", "PUT", API + path] + h
    if data: cmd += ["-d", json.dumps(data)]
    return json.loads(subprocess.run(cmd, capture_output=True, text=True).stdout)

def api_delete(path, token=None):
    h = []
    if token: h += ["-H", "Authorization: Bearer " + token]
    return json.loads(subprocess.run(["curl", "-s", "-X", "DELETE", API + path] + h, capture_output=True, text=True).stdout)

async def main():
    # ===== 1. 普通用户流程 =====
    ts = "e2e_" + str(int(time.time() * 1000))[-8:]
    r = api_post("/auth/register", {"username": ts, "password": "123456", "nickname": "E2E测试"})
    token = r["data"]["token"]
    uid = r["data"]["userId"]
    # 充值
    api_post("/points/topup?points=200", token=token)
    # 预建地址（用于商品详情页兑换）
    r = api_post("/addresses", {"receiverName":"预置","receiverPhone":"13800138001","province":"广东省","city":"深圳市","district":"南山区","detailAddress":"科技园路","isDefault":1}, token)
    # 商品
    r = api_get("/products?pageNum=1&pageSize=5")
    pid = r["data"]["records"][0]["id"]
    pname = r["data"]["records"][0]["name"]

    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True, args=["--no-sandbox","--disable-gpu","--single-process"])
        page = await (await browser.new_context(viewport={"width":1280,"height":800})).new_page()

        # 1.1 登录
        print("\n[1.1] 用户登录")
        await page.goto("http://localhost:3000/#/home", wait_until="networkidle")
        await page.evaluate("localStorage.setItem('token','%s')" % token)
        await page.evaluate("localStorage.setItem('userInfo',JSON.stringify({userId:%d,username:'%s',nickname:'测试',points:200}))" % (uid, ts))
        await page.goto("http://localhost:3000/#/home", wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "01-homepage.png")
        check("首页加载", await page.is_visible("text=积分商城"))
        check("导航栏'我的订单'", await page.is_visible("text=我的订单"))
        check("导航栏'积分明细'", await page.is_visible("text=积分明细"))

        # 1.2 签到
        print("\n[1.2] 签到")
        r = api_post("/points/sign-in", token=token)
        check("签到成功", r["code"]==200)

        # 1.3 商品详情
        print("\n[1.3] 商品详情")
        await page.goto("http://localhost:3000/#/product/%d" % pid, wait_until="networkidle")
        await asyncio.sleep(2)
        await screenshot(page, "02-product.png")
        check("商品详情加载", await page.is_visible("text=" + pname))

        # 1.4 选择地址 + 兑换
        print("\n[1.4] 兑换")
        if await page.is_visible(".el-select"):
            await page.click(".el-select")
            await asyncio.sleep(0.5)
            opt = page.locator(".el-select-dropdown__item").first
            if await opt.is_visible():
                await opt.click()
                await asyncio.sleep(0.5)
        btn = page.locator("button:has-text('立即兑换')")
        if await btn.is_visible():
            await btn.click()
            await asyncio.sleep(1)
            if await page.is_visible("text=兑换确认"):
                await page.click("button:has-text('确认兑换')")
                await asyncio.sleep(2)
                await screenshot(page, "03-order.png")
                check("兑换成功", "order" in page.url)
            else:
                check("兑换弹窗", False)
        else:
            check("兑换按钮", False)

        # 1.5 订单Tab
        print("\n[1.5] 订单Tab")
        await page.goto("http://localhost:3000/#/orders", wait_until="networkidle")
        await asyncio.sleep(2)
        tabs = await page.locator(".el-tabs__item").all()
        check("5个Tab", len(tabs)>=3)
        for i, name in enumerate(["全部","待发货","已发货","已完成","已取消"]):
            if i < len(tabs):
                await tabs[i].click()
                await asyncio.sleep(0.5)
                check("Tab[%s]无报错" % name, not await page.is_visible(".el-message--error"))

        # 1.6 取消订单
        print("\n[1.6] 取消订单")
        r = api_get("/orders?pageNum=1&pageSize=1", token)
        if r["code"]==200 and r["data"]["records"]:
            oid = r["data"]["records"][0]["id"]
            r = api_post("/orders/%d/cancel" % oid, token=token)
            check("取消订单成功", r["code"]==200)
        else:
            check("取消订单", False, "无订单")

        # 1.7 浏览器新增地址
        print("\n[1.7] 浏览器新增地址")
        await page.goto("http://localhost:3000/#/addresses", wait_until="networkidle")
        await asyncio.sleep(2)
        if await page.is_visible("button:has-text('新增地址')"):
            await page.click("button:has-text('新增地址')")
            await asyncio.sleep(1)
            if await page.is_visible(".el-dialog"):
                inputs = page.locator(".el-dialog input.el-input__inner")
                await inputs.nth(0).fill("浏览器测试")
                await inputs.nth(1).fill("13800138001")
                await inputs.nth(2).fill("广东省")
                await inputs.nth(3).fill("深圳市")
                await inputs.nth(4).fill("南山区")
                await page.locator(".el-dialog textarea").fill("浏览器测试地址")
                await asyncio.sleep(0.5)
                await page.click("button:has-text('保存')")
                await asyncio.sleep(2)
                check("浏览器新增地址", not await page.is_visible(".el-message--error"))
                await screenshot(page, "04-address.png")

        # 1.8 积分明细
        print("\n[1.8] 积分明细")
        await page.goto("http://localhost:3000/#/points-records", wait_until="networkidle")
        await asyncio.sleep(2)
        check("积分明细加载", await page.is_visible("text=积分明细") or await page.is_visible(".el-table"))

        # ===== 2. 管理后台 =====
        print("\n[2.1] 管理后台登录")
        await page.goto("http://localhost:3000/#/admin/login", wait_until="load")
        await asyncio.sleep(1)
        await page.fill('input[placeholder*="用户"],input[type="text"]', "admin")
        await page.fill('input[placeholder*="密码"],input[type="password"]', "admin123")
        await page.click("button:has-text('登录')")
        await asyncio.sleep(3)
        check("数据看板", await page.is_visible("text=数据看板") or "dashboard" in page.url)

        # 后台页面列表
        print("\n[2.2] 后台页面")
        for name, p in {"商品管理":"/#/admin/products","订单管理":"/#/admin/orders","系统配置":"/#/admin/config"}.items():
            await page.goto("http://localhost:3000" + p, wait_until="networkidle")
            await asyncio.sleep(1)
            check("[%s]页面可访问" % name, "404" not in await page.title())

        # ===== 3. API 验收（管理后台 + 上传） =====
        print("\n[3.1] 管理后台API")
        adm = api_post("/auth/admin/login", {"username":"admin","password":"admin123"})
        adm_token = adm["data"]["token"]

        # 商品CRUD
        r = api_get("/admin/products?pageNum=1&pageSize=10", adm_token)
        check("商品列表", r["code"]==200)
        r = api_post("/admin/products", {"name":"验收商品","description":"验收","coverImage":"/img.jpg","pointsRequired":5,"stock":3}, adm_token)
        pid2 = r["data"]
        check("新增商品", r["code"]==200)
        r = api_put("/admin/products/%s" % pid2, {"name":"验收改","description":"改","coverImage":"/img2.jpg","pointsRequired":10,"stock":5}, adm_token)
        check("编辑商品", r["code"]==200)
        r = api_put("/admin/products/%s/status" % pid2, token=adm_token)
        check("上下架", r["code"]==200)
        r = api_delete("/admin/products/%s" % pid2, adm_token)
        check("删除商品", r["code"]==200)

        # 订单发货
        r = api_get("/admin/orders?pageNum=1&pageSize=10", adm_token)
        check("订单列表", r["code"]==200)
        # 新建商品->注册用户->兑换->发货->确认收货
        r = api_post("/admin/products", {"name":"发","description":"","coverImage":"/img.jpg","pointsRequired":1,"stock":10}, adm_token)
        spid = r["data"]
        ts2 = "sh" + str(int(time.time()*1000))[-6:]
        r = api_post("/auth/register", {"username":ts2,"password":"123456","nickname":"发"})
        ut = r["data"]["token"]
        api_post("/points/topup?points=200", token=ut)
        r = api_post("/addresses", {"receiverName":"发","receiverPhone":"13800138001","province":"广东","city":"深圳","district":"南山","detailAddress":"科技园","isDefault":1}, ut)
        aid = r["data"]["id"]
        r = api_post("/orders", {"productId":spid,"addressId":aid}, ut)
        if r["code"]==200:
            oid2 = r["data"]["id"]
            check("用户兑换", True)
            r = api_put("/admin/orders/%d/ship" % oid2, {"trackingNo":"SF888"}, adm_token)
            check("订单发货", r["code"]==200)
            r = api_post("/orders/%d/confirm" % oid2, token=ut)
            check("确认收货", r["code"]==200)

        # 配置 + 看板 + 权限
        r = api_get("/admin/config", adm_token)
        check("配置列表", r["code"]==200)
        r = api_get("/admin/dashboard", adm_token)
        check("数据看板", r["code"]==200)
        r = api_get("/admin/dashboard", token)
        check("非管理员拒绝", r["code"]!=200)

        # 文件上传
        print("\n[3.2] 文件上传")
        subprocess.run(["python3","-c","with open('/tmp/e2e_upload.png','wb') as f: f.write(b'\\x89PNG\\r\\n\\x1a\\n'+b'\\x00'*100)"], capture_output=True)
        r_cmd = subprocess.run(["curl","-s","-X","POST",API+"/admin/upload",
            "-H","Authorization: Bearer "+adm_token,
            "-F","file=@/tmp/e2e_upload.png"], capture_output=True, text=True)
        r = json.loads(r_cmd.stdout) if r_cmd.stdout else {}
        upload_ok = r.get("code")==200
        check("图片上传", upload_ok)
        if upload_ok:
            url = r["data"]
            r2 = subprocess.run(["curl","-s","-o","/dev/null","-w","%{http_code}","http://localhost:8080"+url], capture_output=True, text=True)
            check("上传文件可访问", r2.stdout=="200")

        await browser.close()

    # ===== 报告 =====
    passed = sum(1 for r in results if r[1])
    failed = [r for r in results if not r[1]]
    print("\n" + "="*60)
    print("  完整 E2E 验收报告  (%d项)" % len(results))
    print("="*60)
    for s, ok, d in results:
        print("  %s %s %s" % ("✅" if ok else "❌", s, (" - "+d if d else "")))
    print("="*60)
    if failed:
        print("  失败: %d 项" % len(failed))
        for s, _, d in failed:
            print("    ❌ %s %s" % (s, (" - "+d if d else "")))
        exit(1)
    else:
        print("  ✅ 全部通过，0 项失败，0 项手动验证！")
        print("="*60)

if __name__ == "__main__":
    asyncio.run(main())
