---
id: SIGMA-SCP-WHOLE-WINDOW-HANDOFF-WIN20260911-SIGMA-PRIVATE-001
artifact_type: navigation
theory_family: private-reader-app-continuity
lifecycle: active
evidence_level: session-checkpoint-user-terminal-plus-repository-revalidated
runtime_priority: supporting
document_revision: 1
created: 2026-09-11
updated: 2026-09-11
source_window_id: WIN-20260911-SIGMA-PRIVATE-001
source_platform: chatgpt
source_platform_thread_id: unavailable-in-current-harness
source_window_event_at: 2026-09-11T12:29:59Z
settled_by_platform: chatgpt
reading_roles:
  - navigation
  - fresh-window-handoff
epistemic_identity:
  - whole-window-handoff-checkpoint
  - user-terminal-plus-repository-revalidated
  - semantically-sufficient
  - not-chat-summary
  - not-upstream-authority
summary_guard: true
---

# Sigma 私人版整窗交接认知检查点 001

英文检索别名：

> **Whole-window Handoff Cognitive Checkpoint — WIN-20260911-SIGMA-PRIVATE-001**

本文件不是平均压缩聊天摘要。

它负责让新窗口直接恢复：

> **这个私人 App 到底在解决什么；第一版真机暴露了哪些问题；末轮已经怎样返工；当前仓库哪些已经成立、哪些仍然只是“代码看起来修了但真机未验证”；下一步为什么必须继续调通已选站点，而不是继续扩站、堆社区源或重写 Sigma。**

---

# 0. Handoff FCC — 第一屏任务位置图

~~~text
Project
Penrix/legado-E
= 以 阅读Sigma / Legado 生态为底座的私人 Android 内容 App

Base
Luoyacheng/legado-E
main 保持上游基线，不直接污染

Active Work Branch
private-site-platform

Code Head Before Recovery-package Write
ee3d1e8ab18ee75709586a9ad9f55c5155327760

Latest Revalidated CI
Private CI #27
run 34598125581
SUCCESS

Current Product Goal
把真正有价值、自己会长期用的网站私有化进同一个 App
↓
小说尽量进入 Sigma 原生搜索 / 书架 / 阅读 / TTS
视频与特殊站点走干净、稳定、登录态隔离的 App 内 WebView
↓
减少切 App / 切网页
↓
正文不念广告，视频不弹广告、不乱跳站

Current Work Block
第一轮真机反馈后的“可用性返工”
!= 扩站
!= 做大而全源库
!= 重写 Sigma

Current Reasoning Edge
工程能编译
!= 产品适配完成

站点能打开
!= 原生搜索 / 阅读可用

有 BookSource JSON
!= 搜得到、点得开、正文正确

下一步必须按真机验收链逐站闭合
~~~

---

# 1. 冻结的产品哲学

## 1.1 社区源只是矿场，不是产品层

作者已明确纠正：

~~~text
社区书源
= 找宝藏网站 / 找可复用解析思路 / 发现站点变化

社区书源
!= App 里再建一个“社区精选源库”
~~~

真正留下来的站点必须：

~~~text
发现有价值网站
↓
私有化为 Penrix 自己维护的站点 / Pure Adapter
↓
长期由我们自己修搜索、正文、广告、域名、登录
~~~

垃圾站、重复站、低价值站不收集。

用户一个人看不了几十上百个站点，覆盖数量不是目标。

## 1.2 文字站最终验收标准

第一版真机以后，标准已经从“两条简单要求”升级为完整生产链：

~~~text
搜得到
↓
点得开
↓
详情正确
↓
目录完整
↓
正文完整
↓
广告 / 水印 / 收藏提示清掉
↓
繁体站正确转简
↓
连续滚动 / 跨章自然
↓
TTS 不念网站垃圾
~~~

其中任何一环不成立，都不能说“适配完成”。

## 1.3 视频站最终验收标准

~~~text
能打开
↓
能播放
↓
播放器不被净化误杀
↓
广告 / 悬浮层 / 弹窗尽量清理
↓
当前 WebView 不被带去广告站
↓
不同站登录态不互相污染
~~~

不绕 DRM、会员、年龄验证或站点正常权限。

## 1.4 UI 主方向

底部四栏继续保留 Sigma 原结构：

~~~text
书架
发现
订阅
我的
~~~

不新增“视频”或“私人站点”底部栏。

第二栏“发现”现在承担：

~~~text
第一层：私人站点快捷入口 + 搜书
第二层：原 Legado 书源发现（兼容能力）
~~~

“我的 → 私人站点”旧入口已删除。

原发现源保留只为 Legado 生态兼容，不再是私人版的主要导航。

---

# 2. 第一版真机反馈 — 必须保留，不能被 CI 绿灯冲掉

用户对旧测试 APK 的实机反馈：

~~~text
UI
- 私人站点既然内置，不应藏在“我的”
- 发现源作为主要入口不直觉
- 更希望站点是能直接点的一层入口

搜索覆盖
《大时代1958》
- TWKAN 网站有
- App 当时 TWKAN 搜不到
- 几个内置文字站中只有全本小说网能在 App 搜到

《峨眉剑仙》
- 当时只有 TWKAN 能在 App 搜到
- 但结果 / 正文繁体没有正确转简

站点功能
- 第一版主打不开
- 八叉打不开
- UAA 账号密码正确但网页登录失败
- 河图能登录，但具体书打不开
- 69 一直跳人机验证，并容易卡在验证

广告
- Cool18 可用，但还有广告
- Hanime1 能看，但还有广告
- Pornhub 有广告
- XVideos 有广告
- MissAV 最严重，会跳转别的站

河图账号池
- 测试账号登录后签到没有成功
- 用户裁决：账号池 / 自动签到整个去掉，不值得继续复杂化
~~~

这组真机反馈是当前最高价值的产品证据。

以后不能用“规则文件存在”“CI 成功”“网页可达”覆盖这些反馈。

---

# 3. 真机反馈之后已经落地的返工

以下不是仅口头计划；当前仓库可验证到相应代码 / 提交。

## 3.1 第二栏改为一层私人站点入口

`ExploreFragment` 当前已经：

~~~text
搜书
+
PrivateSiteRegistry 中全部站点 Chip
↓
点击直接进入 WebViewActivity

其下仍保留原 Legado explore source 列表
~~~

语义固定：

> 私人站点是一层入口；书源发现是兼容层。

## 3.2 河图账号池 / 自动签到已整个移除

相对旧测试基线 `8df3b33...`，当前分支已经删除：

- `HotuAccountPool.kt`
- `HotuAutoSignIn.kt`
- `HotuSignInClient.kt`
- `HotuSignInJobService.kt`
- `HotuSignInParser.kt`
- `HotuSignInReceiver.kt`
- `PrivateSecretStore.kt`
- 对应 Manifest / UI / 测试

河图现在只保留：

> **单账号正常网页登录 + Cookie 复用 + 正常有权访问内容。**

不要在新窗口重新设计账号池。

## 3.3 TWKAN 搜索与简繁已重写

当前 `twkan-pure.json`：

~~~text
搜索
= 用户原始关键词
+
java.s2t(key) 机械简→繁关键词
两路结果合并

输出
搜索结果 / 详情 / 目录标题 / 正文
= 显式 java.t2s(...)
~~~

这是针对：

- 《大时代1958》网站有、App 搜不到；
- 《峨眉剑仙》能搜到但繁体没有转简。

**但该修复尚需新 APK 真机复测，不能仅凭规则文本宣判完成。**

## 3.4 69 验证循环已撤

当前 69 Pure：

~~~text
全局搜书遇 Cloudflare
→ 不再 startBrowserAwait 强制弹验证
→ 宁可该线路安静失败
→ 不能把整个搜书流程绑死在人机验证
~~~

站点快捷入口仍可让用户主动打开网站。

## 3.5 八叉已按当前站点结构重做

末轮返工已修改 `bachashuku-pure.json`，不再沿用上一版错误结构。

当前状态：

> **代码已返工，仍需真机重新验证搜索 → 登录 → 目录 → 正文。**

不能把“已改规则”写成“已修好”。

## 3.6 河图目录 selector 已纠正

当前 `hotupub-pure.json`：

~~~text
单账号
loginUrl = 官方登录页

目录项
.bookdetails-cataloglist-item

正文
.bookread-content-box
~~~

旧错误 `.bookdetails-catalog-list` 已纠正。

当前状态：

> 登录已在旧 APK 真机成立；新的详情 / 目录 / 正文规则仍需新 APK 实测。

## 3.7 WebView Cookie 全局清理问题已修

第一版发现 Sigma 原先的 WebView session 行为会造成：

~~~text
打开一个站
→ removeSessionCookies(null)
→ 其它站 session 也可能被清
~~~

这会直接伤害 UAA / 河图 / 八叉等登录站。

当前分支已改：

- Cookie 恢复 / 清理按当前站点隔离；
- pooled WebView 不再无差别清全局 session；
- 开启现代登录流程常见的第三方 Cookie 支持。

这是针对 UAA 登录失败的重要系统级修复。

**UAA 仍必须用用户真实账号重新验证；不能提前判 PASS。**

## 3.8 广告与跳站拦截继续加严

当前站点 profile / cleaner 已追加：

- Hanime1：当前已知广告域；
- MissAV：多个广告 / 跳转域；
- 第一版主：站点相关广告网络；
- Cool18 / Pornhub / XVideos：弹层 / 浮层选择器与公共广告网络；
- 顶层导航跳往已知广告域：WebViewClient 原生层直接拒绝；
- `onPageStarted` 再做兜底。

最新代码头提交：

~~~text
ee3d1e8ab18ee75709586a9ad9f55c5155327760
private: block known ad top-level redirects before leaving managed sites
~~~

这直接针对 MissAV “会把当前页带去别站”的真机故障。

---

# 4. Repository Reality Snapshot

~~~text
repository:
Penrix/legado-E

upstream/base main:
8b87c5aba4df91c39a3a0939a68a1180b9f2ee1c

active branch:
private-site-platform

code_head_before_recovery_package:
ee3d1e8ab18ee75709586a9ad9f55c5155327760

code_head_message:
private: block known ad top-level redirects before leaving managed sites

post-old-apk corrective commits:
23 commits after 8df3b33c670428fedea54c48de991b294e1ecdc0

latest Private CI for code head:
run #27
run_id 34598125581
status SUCCESS

important correction:
terminal conversation text said “还没重新跑最终 CI”
→ this became stale before recovery settlement
→ repository reality wins
~~~

当前 `main` 没有合并私人施工。

旧 Draft PR：

- PR #1：TWKAN 第一阶段旧实验；
- PR #2：视频站识别旧实验；

它们是形成史 / 原型，不是当前 live work branch。

不要从 PR #1 或 #2 恢复主线。

---

# 5. 当前仍未闭合的关键问题

## 5.1 第一版主是当前最明确的仓库—裁决不一致

末轮裁决：

~~~text
第一版主当前原生正文涉及字符占位 / 解码
↓
若拿不到完整可靠规则
→ 暂时禁用原生 BookSource
→ 只保留一级网站入口
~~~

但当前仓库 `diyibanzhu-pure.json` 仍然：

~~~text
enabled = true
source = 111bz.cc
content = id.content@text
~~~

即：**裁决还没有真正落到仓库。**

这是新窗口第一优先级。

下一步只能二选一：

1. 把当前第一版主 / 镜像真正需要的解码链恢复并实测；
2. 在可靠规则完成前禁用原生搜索源，只保留快捷网站入口。

不允许继续维持“看起来有源，实际打不开”的状态。

## 5.2 UAA 登录仍是未验证问题

已做 Cookie 隔离与第三方 Cookie 修复。

但用户真实账号尚未在修复后 APK 上复测。

因此状态：

~~~text
root-cause hypothesis improved
code-level fix exists
real-device validation pending
~~~

如果仍失败，下一步应观察：

- 官方登录页验证码 / challenge；
- WebView storage / SameSite / third-party cookie；
- 登录成功后 Cookie 是否回写 CookieJar；
- API / HTML 是否使用同一认证状态。

不要再次默认“账号密码错”。

## 5.3 搜书覆盖必须以真实冷门书验证

至少固定：

~~~text
《大时代1958》 青山铁杉
《峨眉剑仙》 北冥没有鱼啊
~~~

以及已有成人站 canary：

~~~text
《我在三国当混蛋》 三年又三年
《大唐双龙之重生边不负》 wolui
《渔港春夜》 棺材里的笑声
~~~

网站库存与 App 搜索是两件事。

验收要明确记录：

> 网站有 + App 无 = Adapter 搜索缺陷。

## 5.4 广告是当前重点，不是附属优化

上一版用户已经明确指出：

- Cool18 有广告；
- Hanime1 有广告；
- Pornhub 有广告；
- XVideos 有广告；
- MissAV 会跳站。

当前规则已经加严，但必须真机重新压。

视频站不能以“能播放”作为完成。

---

# 6. 当前不要做的事

新窗口恢复后禁止先做：

1. 继续扩新小说站 / 视频站；
2. 重新建立“社区精选源”产品层；
3. 恢复河图多账号池或自动签到；
4. 新增底部“视频 / 私人站点”Tab；
5. 为了统一架构大改 AnalyzeRule / Rhino / java.ajax / BackstageWebView；
6. 把旧 APK 的失败当成站点本身无资源；
7. 因为 CI 绿就直接宣布适配完成；
8. 把第一版主当前 enabled 的旧规则当成已解决；
9. 继续让 69 全局搜书强制弹人机验证；
10. 自动绕过会员 / 点数 / DRM / 地区 / 年龄权限。

---

# 7. 下一合法动作

恢复后优先级：

~~~text
P0
第一版主
→ 完成可靠解码并实测
OR
→ 先禁用原生 BookSource，只保留网页入口

P0
基于当前 ee3d1e8 之后的代码重新产出测试 APK
→ 不再使用旧 8df3b33 测试包

P1
真机验证搜索
《大时代1958》
《峨眉剑仙》
+ 成人 canary

P1
真机验证会员站
UAA 登录
八叉登录 / 目录 / 正文
河图单账号详情 / 目录 / 正文

P1
真机验证广告
Cool18
Hanime1
Pornhub
XVideos
MissAV
重点看 MissAV 是否还能顶层跳站

P2
逐个记录失败点
只修具体站点 / 具体链路
不要重新大施工
~~~

---

# 8. 新窗口第一屏恢复口令

新窗口应先读：

1. `working/session-continuity/windows/WIN-20260911-SIGMA-PRIVATE-001/WHOLE-WINDOW-HANDOFF-CHECKPOINT-001.md`
2. `working/session-continuity/windows/WIN-20260911-SIGMA-PRIVATE-001/WINDOW-RECOVERY-MANIFEST-001.md`
3. `working/session-continuity/windows/WIN-20260911-SIGMA-PRIVATE-001/SESSION-EVIDENCE-001.md`

然后读取当前 `private-site-platform` live state，而不是相信恢复包中的 SHA 永远不变。

最小启动判断：

> **这是旧窗口连续施工，不是 Fresh Onboarding。恢复包负责恢复工作记忆；GitHub 当前分支负责最新事实。先 revalidate branch head / CI / 第一版主状态，再从 P0 继续。**
