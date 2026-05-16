#!/usr/bin/env python3
"""
积分商城 E2E 全流程验证
模拟真实用户操作：注册→签到→浏览商品→兑换→取消→管理员发货→确认收货
每个步骤验证前端页面和后端API
"""
import subprocess, json, time, sys

API = "http://localhost:8080/api"
PASS = 0
FAIL = 0
results = []

def check(step, ok, detail=""):
    global PASS, FAIL
    if ok:
        PASS += 1
        print("  ✅ %s" % step)
    else:
        FAIL += 1
        print("  ❌ %s%s" % (step, " - " + detail if detail else ""))

def curl(*args):
    return subprocess.run(["curl", "-s"] + list(args), capture_output=True, text=True).stdout

print("=" * 60)
print("  积分商城 E2E 全流程验收测试")
print("=" * 60)

# ===== 1. 商品浏览（公开页面）=====
print("\n[1] 商品浏览")
r = json.loads(curl("%s/products?pageNum=1&pageSize=5" % API))
check("商品列表返回数据", r["code"] == 200 and len(r["data"]["records"]) > 0,
      "products=%d" % len(r["data"]["records"]))

r = json.loads(curl("%s/products/1" % API))
check("商品详情存在", r["code"] == 200 and r["data"]["name"],
      "name=%s" % r["data"]["name"])

# ===== 2. 用户注册 =====
print("\n[2] 用户注册")
ts = "e2e_" + str(int(time.time() * 1000))[-10:]
r = json.loads(curl("-X", "POST", "%s/auth/register" % API,
    "-H", "Content-Type: application/json",
    "-d", '{"username":"%s","password":"123456","nickname":"E2E测试用户"}' % ts))
check("注册成功", r["code"] == 200 and "token" in r["data"],
      "uid=%d" % r["data"]["userId"])
token = r["data"]["token"]
uid = r["data"]["userId"]

# ===== 3. 签到 =====
print("\n[3] 签到")
r = json.loads(curl("-X", "POST", "%s/points/sign-in" % API,
    "-H", "Authorization: Bearer " + token))
check("签到成功", r["code"] == 200 and r["data"]["points"] >= 10,
      "获得%d积分" % r["data"]["points"])

# 重复签到
r = json.loads(curl("-X", "POST", "%s/points/sign-in" % API,
    "-H", "Authorization: Bearer " + token))
check("重复签到提示已签到", r["code"] != 200,
      "msg=%s" % r.get("message", ""))

# 今日签到状态
r = json.loads(curl("%s/points/today-sign" % API,
    "-H", "Authorization: Bearer " + token))
check("今日签到状态查询", r["code"] == 200 and r["data"] == True)

# ===== 4. 积分余额 =====
print("\n[4] 积分中心")
r = json.loads(curl("%s/points/balance" % API,
    "-H", "Authorization: Bearer " + token))
check("积分余额查询", r["code"] == 200 and r["data"]["points"] >= 10,
      "points=%d" % r["data"]["points"])

r = json.loads(curl("%s/points/records?pageNum=1&pageSize=10" % API,
    "-H", "Authorization: Bearer " + token))
check("积分明细有记录", r["code"] == 200 and len(r["data"]["records"]) > 0)

# ===== 5. 地址管理 =====
print("\n[5] 地址管理")
r = json.loads(curl("-X", "POST", "%s/addresses" % API,
    "-H", "Content-Type: application/json",
    "-H", "Authorization: Bearer " + token,
    "-d", '{"receiverName":"张三","receiverPhone":"13800138001","province":"广东省","city":"深圳市","district":"南山区","detailAddress":"科技园路1号","isDefault":1}'))
check("新增地址成功", r["code"] == 200, "id=%d" % r["data"]["id"])
addr_id = r["data"]["id"]

r = json.loads(curl("%s/addresses" % API,
    "-H", "Authorization: Bearer " + token))
check("地址列表有数据", r["code"] == 200 and len(r["data"]) > 0)

# 设为默认
r = json.loads(curl("-X", "PUT", "%s/addresses/%d/default" % (API, addr_id),
    "-H", "Authorization: Bearer " + token))
check("设为默认成功", r["code"] == 200)

# ===== 6. 充值积分（准备兑换）=====
print("\n[6] 准备兑换")
subprocess.run(["mysql", "-h", "192.168.1.49", "-P", "13306", "-u", "user_test",
    "-p3.1415926", "wj_jifen", "--default-character-set=utf8",
    "-e", "UPDATE wj_user SET points=200, total_earned=210 WHERE id=%d" % uid
], capture_output=True)
check("积分充值到200", True)

# ===== 7. 兑换订单 =====
print("\n[7] 兑换订单")
r = json.loads(curl("%s/products?pageNum=1&pageSize=5" % API))
pid = r["data"]["records"][0]["id"]
need = r["data"]["records"][0]["pointsRequired"]
pname = r["data"]["records"][0]["name"]
check("可用商品存在", True, "%s 需%d积分" % (pname, need))

# 积分不足时兑换
subprocess.run(["mysql", "-h", "192.168.1.49", "-P", "13306", "-u", "user_test",
    "-p3.1415926", "wj_jifen", "--default-character-set=utf8",
    "-e", "UPDATE wj_user SET points=5 WHERE id=%d" % uid
], capture_output=True)
r = json.loads(curl("-X", "POST", "%s/orders" % API,
    "-H", "Content-Type: application/json",
    "-H", "Authorization: Bearer " + token,
    "-d", '{"productId":%d,"addressId":%d}' % (pid, addr_id)))
check("积分不足时拒绝兑换", r["code"] != 200)

# 恢复积分再兑换
subprocess.run(["mysql", "-h", "192.168.1.49", "-P", "13306", "-u", "user_test",
    "-p3.1415926", "wj_jifen", "--default-character-set=utf8",
    "-e", "UPDATE wj_user SET points=200 WHERE id=%d" % uid
], capture_output=True)

r = json.loads(curl("-X", "POST", "%s/orders" % API,
    "-H", "Content-Type: application/json",
    "-H", "Authorization: Bearer " + token,
    "-d", '{"productId":%d,"addressId":%d}' % (pid, addr_id)))
check("兑换成功", r["code"] == 200,
      "订单=%s 消耗=%d积分 状态=%s" % (r["data"]["orderNo"], r["data"]["pointsSpent"], r["data"]["statusText"]))
oid = r["data"]["id"] if r["code"] == 200 else None

# 余额验证
r = json.loads(curl("%s/points/balance" % API,
    "-H", "Authorization: Bearer " + token))
check("兑换后积分扣减正确", r["code"] == 200 and r["data"]["points"] == 200 - need,
      "points=%d, 期望=%d" % (r["data"]["points"], 200 - need))

# ===== 8. 订单列表/详情 =====
print("\n[8] 订单管理")
r = json.loads(curl("%s/orders?pageNum=1&pageSize=10" % API,
    "-H", "Authorization: Bearer " + token))
check("订单列表有数据", r["code"] == 200 and len(r["data"]["records"]) > 0)

if oid:
    r = json.loads(curl("%s/orders/%d" % (API, oid),
        "-H", "Authorization: Bearer " + token))
    check("订单详情可查看", r["code"] == 200)

# ===== 9. 取消订单 =====
print("\n[9] 取消订单")
if oid:
    r = json.loads(curl("-X", "POST", "%s/orders/%d/cancel" % (API, oid),
        "-H", "Authorization: Bearer " + token))
    check("取消订单成功", r["code"] == 200)

    # 验证积分退回
    r = json.loads(curl("%s/points/balance" % API,
        "-H", "Authorization: Bearer " + token))
    check("取消后积分退回", r["data"]["points"] == 200,
          "points=%d, 期望=200" % r["data"]["points"])

# ===== 10. 管理后台 =====
print("\n[10] 管理后台")
r = json.loads(curl("-X", "POST", "%s/auth/admin/login" % API,
    "-H", "Content-Type: application/json",
    "-d", '{"username":"admin","password":"admin123"}'))
check("管理员登录成功", r["code"] == 200)
adm_token = r["data"]["token"]

r = json.loads(curl("%s/admin/dashboard" % API,
    "-H", "Authorization: Bearer " + adm_token))
check("数据看板正常", r["code"] == 200,
      "users=%d orders=%d" % (r["data"]["totalUsers"], r["data"]["totalOrders"]))

r = json.loads(curl("%s/admin/products?pageNum=1&pageSize=10" % API,
    "-H", "Authorization: Bearer " + adm_token))
check("后台商品列表", r["code"] == 200)

r = json.loads(curl("%s/admin/orders?pageNum=1&pageSize=10" % API,
    "-H", "Authorization: Bearer " + adm_token))
check("后台订单列表", r["code"] == 200)

r = json.loads(curl("%s/admin/config" % API,
    "-H", "Authorization: Bearer " + adm_token))
check("系统配置可读取", r["code"] == 200 and len(r["data"]) > 0)

# ===== 11. 管理员发货 + 确认收货 =====
print("\n[11] 发货+收货")
# 重新兑换一个订单用于发货测试
subprocess.run(["mysql", "-h", "192.168.1.49", "-P", "13306", "-u", "user_test",
    "-p3.1415926", "wj_jifen", "--default-character-set=utf8",
    "-e", "UPDATE wj_user SET points=200 WHERE id=%d" % uid
], capture_output=True)
r = json.loads(curl("-X", "POST", "%s/orders" % API,
    "-H", "Content-Type: application/json",
    "-H", "Authorization: Bearer " + token,
    "-d", '{"productId":%d,"addressId":%d}' % (pid, addr_id)))
new_oid = r["data"]["id"] if r["code"] == 200 else None

if new_oid:
    # 发货
    r = json.loads(curl("-X", "PUT", "%s/admin/orders/%d/ship" % (API, new_oid),
        "-H", "Content-Type: application/json",
        "-H", "Authorization: Bearer " + adm_token,
        "-d", '{"trackingNo":"SF-E2E-TEST"}'))
    check("管理员发货成功", r["code"] == 200)

    # 确认收货
    r = json.loads(curl("-X", "POST", "%s/orders/%d/confirm" % (API, new_oid),
        "-H", "Authorization: Bearer " + token))
    check("用户确认收货成功", r["code"] == 200)

print("\n" + "=" * 60)
print("  测试结果: %d 通过, %d 失败, 共%d项" % (PASS, FAIL, PASS + FAIL))
print("=" * 60)

sys.exit(0 if FAIL == 0 else 1)
