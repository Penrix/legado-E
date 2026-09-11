---
id: SIGMA-SCP-SESSION-EVIDENCE-WIN20260911-SIGMA-PRIVATE-001
artifact_type: evidence
theory_family: private-reader-app-continuity
lifecycle: active
evidence_level: selected-session-evidence
runtime_priority: supporting
document_revision: 1
created: 2026-09-11
updated: 2026-09-11
source_window_id: WIN-20260911-SIGMA-PRIVATE-001
source_platform: chatgpt
source_platform_thread_id: unavailable-in-current-harness
source_window_event_at: 2026-09-11T12:29:59Z
reading_roles:
  - evidence
epistemic_identity:
  - selected-session-evidence
  - semantic-recovery-anchor
  - user-correction-preserving
  - repository-revalidated
  - not-complete-transcript
  - not-upstream-authority
summary_guard: false
---

# Sigma 私人版窗口会话证据 001

英文检索别名：

> **Session Evidence — WIN-20260911-SIGMA-PRIVATE-001**

本文件不复刻整段聊天。

只保存如果丢掉，就最容易让新窗口再次走错的用户裁决、真机证据、工程边界与 nearest-wrong。

---

# 1. 用户真正要的不是“更多网站”，而是“少量高价值网站被 App 吃掉”

窗口中最重要的产品纠正之一：

> **“我是从社区源里找到宝藏网站，然后把这些网站私有化，就跟台湾小说网一样，垃圾网站不用收集，只保留有价值的。毕竟我一个人也看不了那么多网站。”**

因此：

~~~text
社区源生态
= 采矿场

宝藏网站
= 长期资产候选

Pure Adapter
= 私人版真正产品能力

社区源数量
!= 产品价值
~~~

不再建立“Penrix 社区精选”这种中间产品层。

UU、全本、Cool18 等一旦被选中，就应该以 Penrix 自己维护的 Pure 站点身份存在，而不是继续把“社区来源”暴露成产品概念。

---

# 2. 两条最初简单标准仍然成立，但真机以后被展开了

用户最早把文字站价值压成：

> **搜得到；正文没广告。**

不是要评分、排行榜、完整度仪表盘。

但第一次 APK 真机测试说明，这两条在工程上实际展开为：

~~~text
搜得到
= 站点有这本书时，App 搜索链真的能返回

正文没广告
= 点得开 + 目录对 + 正文全 + 不漏页 + 不念推广 + 简体正确
~~~

所以后续不能用“网站库存存在”替代“App 搜索成功”，也不能用“DOM 看起来干净”替代 TTS 验收。

---

# 3. 连续阅读：网站分页不是产品体验

用户明确指出：

> **“看书直接一直往下划，不用点下一页……点下一页很出戏。”**

窗口确认 Sigma 自己已经能在滚动模式跨章继续下一章。

因此统一原则：

> **网站可以分页，App 不分页。**

分权：

~~~text
网站章节之间
→ Sigma 原生连续滚动解决

同一章被网站拆成 1/2/3 页
→ Adapter 的 nextContentUrl / 抓取逻辑先拼完整
~~~

不要给每个站复制网页“下一页 / 下一章”交互。

---

# 4. 河图账号池是一次明确淘汰，不是未来 TODO

用户曾提出：

- 多个河图账号；
- 每天自动签到；
- 点数累积。

窗口一度真的实现：

- 多账号 Cookie 池；
- Keystore；
- Alarm / JobScheduler；
- 自动签到 parser；
- 管理 UI。

但第一次真机结果：

> **登录一个测试账号后也没有签到成功；用户直接裁决“这个号池还是去掉吧，太麻烦了”。**

当前仓库已经整套删除。

因此：

~~~text
河图账号池
= closed / rejected

自动签到
= closed / rejected

未来除非用户重新明确开启
→ 不得以“之前已经写了很多”为理由恢复
~~~

河图当前只做单账号正常登录与正常权限阅读。

---

# 5. UI 的关键纠正：私人站点必须是一层可点入口

第一版把私人站点放在“我的”里。

用户反馈：

> **“不符合直觉，你都把私人站点内置了，那发现源还有什么用？……总得来说最好还是在能直接点的一级菜单好一点。”**

同时用户认可 Sigma 原四栏：

~~~text
书架
发现源
订阅源
我的
~~~

不需要新增底部 Tab。

当前修复语义：

~~~text
第二栏
├─ 第一层：搜书 + 私人站点快捷入口
└─ 第二层：书源发现（兼容）
~~~

原发现源不是删除，而是降级为生态兼容能力。

这是产品信息架构裁决，不是临时 UI 偏好。

---

# 6. 真机搜索证据：同站网页有书，但 App 搜不到

这是目前最重要的搜索证据。

## 6.1 《大时代1958》

用户实测：

~~~text
TWKAN 网站
= 有

旧 APK TWKAN Pure
= 搜不到

旧 APK 多个内置文字源
= 只有全本小说网搜到
~~~

因此“站点有书”与“Adapter 搜索成功”必须分开测。

## 6.2 《峨眉剑仙》

用户实测：

~~~text
旧 APK
= 只有 TWKAN 能搜到

但 TWKAN
= 繁体没有正确转简
~~~

这两本因此成为主力冷门搜索 canary。

## 6.3 已有成人站 canary

窗口前半段筛站时已经形成：

~~~text
《我在三国当混蛋》 三年又三年
→ 冷门长篇覆盖

《大唐双龙之重生边不负》 wolui
→ 经典同人 / 老文覆盖

《渔港春夜》 棺材里的笑声
→ 老牌长篇档案覆盖
~~~

这些仍可用于验证第一版主 / 八叉 / 成人站库存与完整性。

---

# 7. TWKAN 的失败不是“繁体站天然如此”，而是旧规则设计不够

旧版只做：

~~~text
key
→ java.s2t(key)
→ 单路搜索
~~~

真机反馈说明这不足以覆盖 TWKAN 的混合索引。

当前仓库已改成：

~~~text
原始 key 搜索
+
简→繁 key 第二次搜索
↓
合并结果
~~~

并在：

- Search name；
- Search author；
- intro；
- BookInfo；
- chapterName；
- title；
- content

明确调用 `java.t2s`。

该修复是有明确故障证据驱动的，不是“优化一下”。

但仍需新的真机 APK 才能结算。

---

# 8. 69 的正确失败方式：安静失败，不绑架全局搜索

旧版为了绕 Cloudflare，搜索遇 challenge 会拉起浏览器验证。

用户真机结果：

> **“69书吧一直跳人机验证，还老是卡这。”**

这使一个不稳定线路绑架了整个统一搜索体验。

当前纠正：

~~~text
全局搜书
↓
69 被 CF 拦
↓
该线路返回失败 / 无结果
↓
其它线路继续
~~~

只有用户主动点 69 网站入口时，才自己处理网站验证。

稳定搜索体验优先于“每次强行让 69 出结果”。

---

# 9. 登录站问题不能简单归因于账号

## 9.1 UAA

用户真机：

> **账号密码确认正确，但 UAA 登录不了。**

末轮分析没有把责任推给用户账号，而是发现 WebView / Cookie 的系统性问题。

Sigma 原逻辑存在全局 session 清理风险：

~~~text
打开站点 A
→ 全局 removeSessionCookies
→ 站点 B 登录态被清
~~~

当前仓库已改：

- per-site Cookie 处理；
- pooled WebView Cookie 生命周期修正；
- third-party cookies 开启。

这些是针对 UAA / 八叉 / 河图的基础认证修复。

但 UAA 修后尚未用户真机复测。

## 9.2 河图

旧 APK：

~~~text
网页登录成功
但具体书打不开
~~~

根因找到的是目录 selector 写错，不是登录本身。

当前已改为 `.bookdetails-cataloglist-item`。

## 9.3 八叉

旧 APK：打不开。

末轮已按当前 `bachashuku.org` 结构重做规则。

仍需要真机验证，不得提前写 PASS。

---

# 10. 第一版主：必须保留“宁可暂时无原生源，也不要假适配”的裁决

用户很早就指出第一版主域名经常漂移。

窗口建立：

~~~text
siteId = diyibanzhu
!= 某一个域名
~~~

域名漂移应该由站点 profile / resolver 处理。

但真机又暴露另一层：

> 第一版主原生正文不是普通 `#content` selector 就够，存在字符占位 / 解码问题。

末轮裁决：

~~~text
完整可靠解码规则拿到
→ 才启用原生 BookSource

拿不到
→ 暂时禁用原生 BookSource
→ 一级网站入口照常保留
~~~

当前仓库仍然 `enabled=true`，因此这条裁决还没真正结算。

新窗口必须把它当 P0，而不是遗忘。

---

# 11. 广告不是 polish，而是核心功能

用户选择做私人 App 的重要原因之一就是广告和 TTS。

文字站：

~~~text
网页广告文字
↓
若进入正文
↓
TTS 会真的念出来
~~~

视频站：

~~~text
悬浮广告 / popunder / top-level redirect
↓
直接破坏“统一 App 内观看”
~~~

旧 APK 真机：

- Cool18：仍有广告；
- Hanime1：能看但有广告；
- Pornhub：有广告；
- XVideos：有广告；
- MissAV：最严重，会跳转别站。

末轮广告返工包括：

~~~text
站点级 blockedHostSuffixes
+
站点级 DOM selectors
+
MutationObserver 持续清理
+
click / window.open guard
+
WebViewClient 顶层导航原生拦截
+
onPageStarted 二道兜底
~~~

最新代码头正是广告顶层跳转防护提交。

因此下一版视频验收必须测试“点页面以后会不会突然离开当前站”。

---

# 12. “工程能编译”这条错误已经被真机证伪

第一版曾完成：

~~~text
Kotlin 编译检查
JSON 解析
JS / Rhino 静态检查
Private CI
unit tests
assembleAppDebug
APK artifact
~~~

这些都是真实工程进展。

但真机仍然出现大量产品故障。

所以当前权限关系固定为：

~~~text
CI
= 必要工程 Gate
!= 站点适配验收

真机站点链路
= 产品验收
~~~

文字站验收：

> 搜索 → 详情 → 目录 → 正文 → 净化 → 简体 → 连续滚动 → TTS

视频站验收：

> 打开 → 播放 → 广告 → 跳站 → 登录态

---

# 13. 当前 Repository Reality 对终端消息的一个重要修正

用户贴出的旧窗口最后回答写：

> “还没重新跑最终 CI。”

但恢复时仓库显示：

~~~text
head:
ee3d1e8ab18ee75709586a9ad9f55c5155327760

Private CI #27:
SUCCESS
~~~

因此新窗口不能机械复述“CI 未跑”。

正确恢复方式：

~~~text
终端语义状态
+
当前 Repository Reality
↓
恢复最新工作状态
~~~

这正是本上下文续接协议存在的意义。

---

# 14. 当前 next-action 的真实顺序

~~~text
第一版主仓库缺口
→ 解码成功或禁用原生源

↓

从当前 branch head 生成新 APK

↓

冷门搜索回归
《大时代1958》
《峨眉剑仙》

↓

登录 / 阅读链回归
UAA
八叉
河图

↓

广告回归
Cool18
Hanime1
Pornhub
XVideos
MissAV

↓

只修真实失败点

↓

再次 CI + 新 APK
~~~

不要在这个顺序前插入“继续找更多网站”。

---

# 15. 一句话恢复

> **这个项目已经从“把很多站塞进 Sigma”进入“把少数已经选中的站真正调成私人产品”的阶段；当前最高任务不是扩站，而是用第一轮真机反馈逐站闭合搜索、登录、正文、简繁、广告和跳站，尤其先解决第一版主仓库仍假启用的问题。**
