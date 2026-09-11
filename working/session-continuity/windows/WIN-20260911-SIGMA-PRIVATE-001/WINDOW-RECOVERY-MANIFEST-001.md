---
id: SIGMA-SCP-WINDOW-MANIFEST-WIN20260911-SIGMA-PRIVATE-001
artifact_type: navigation
theory_family: private-reader-app-continuity
lifecycle: active
evidence_level: recovered-window-evidence-map
runtime_priority: supporting
document_revision: 1
created: 2026-09-11
updated: 2026-09-11
source_window_id: WIN-20260911-SIGMA-PRIVATE-001
source_platform: chatgpt
source_platform_thread_id: unavailable-in-current-harness
source_window_event_at: 2026-09-11T12:29:59Z
reading_roles:
  - navigation
  - evidence
epistemic_identity:
  - window-recovery-manifest
  - user-terminal-plus-repository-revalidated
  - semantically-sufficient-recovery
  - not-session-checkpoint
  - not-upstream-authority
summary_guard: false
---

# Sigma 私人版窗口恢复清单 001

英文检索别名：

> **Window Recovery Manifest — WIN-20260911-SIGMA-PRIVATE-001**

本文件不是产品规格，也不是聊天摘要。

它只回答：

> **这个长窗口怎样从“做一个私人阅读 App”走到第一次可安装 APK，再怎样被真机反馈推翻“能编译=可用”的错觉；当前仓库已经把哪些反馈变成代码，哪些还没有闭合；新窗口最小需要读什么才能直接继续。**

---

# 0. 恢复结论

~~~text
Transcript Exactness:
PARTIAL

Reason:
当前恢复不是逐条平台历史取证；
使用本窗口现存上下文 + 用户终端交接消息 + GitHub 当前分支事实。

Semantic Recovery:
SUFFICIENT

Repository Revalidation:
COMPLETED

Source Thread ID:
UNAVAILABLE IN CURRENT HARNESS

Do Not Invent:
不伪造 conversationId
不伪造旧窗口起始时间
不伪造未发生的真机验证
~~~

当前恢复按一个连续工作窗口处理，不拆成“小说站阶段 / 视频站阶段 / 河图阶段”等独立项目。

---

# 1. 窗口主线演化

## 1.1 起点：不是另做一个阅读器，而是把分散内容统一进日用 Sigma

用户真正目标：

~~~text
常用小说站
成人小说站
会员站
论坛
视频站
导航
本地书
↓
尽量统一进一个私人 App
↓
不要在多个 App / 网页之间频繁切换
~~~

选择 Sigma / Legado 不是因为 UI 完美，而是因为：

- 用户本来就在日用；
- 原生书架、搜索、缓存、进度、TTS 已成熟；
- Legado 社区规则生态是巨大适配资产；
- 私人源“大灰狼”明显依赖 Legado 兼容行为。

因此最早就冻结：

> **生态兼容优先于代码洁癖。**

不能为了私人功能重写 AnalyzeRule / Rhino / java.ajax / Cookie / BackstageWebView 等书源运行时核心。

## 1.2 第一阶段：TWKAN 作为最小实验

最初从 TWKAN Pure 入手，验证：

- 用户可见 WebView 的广告净化；
- source verification 绕过；
- 正文广告行清理；
- 花体域名 NFKC 识别；
- 繁转简；
- 不碰通用书源执行器。

形成了 PR #1，但该 PR 只是旧实验形成史。

## 1.3 扩展为“选中的有价值网站私有化”

随后讨论普通小说、成人小说、会员站、论坛与视频站。

重要纠正：

~~~text
不是搜集海量书源
不是做站点评分榜
不是把社区源当一个新产品层

而是：
社区源 / Web / 用户样本
↓
发现长期有价值的网站
↓
私有化
↓
长期维护少数高价值站点
~~~

文字站早期筛选得到：

普通小说重点：
- TWKAN
- 69书吧
- UU看书
- 全本小说

成人 / 会员 / 论坛重点：
- 第一版主
- 八叉书库
- UAA
- 河图文化
- Cool18 / 禁忌书屋

视频 / 导航：
- Hanime1
- Pornhub
- XVideos
- MissAV
- ThePornDude

ViralPorn 因当时状态不稳定未正式纳入。

## 1.4 从网页快捷入口转向“文字站原生化”

目标逐渐明确：

~~~text
文字站
→ 尽量不在网页里读
→ 进入 Sigma Book / Chapter / TextChapter
→ 原生阅读
→ 连续滚动
→ TTS
~~~

网站只负责供内容，App 负责阅读体验。

因此“下一页 / 下一章”网页交互不是目标体验。

同章网站分页需要 Adapter 拼接；章节之间 Sigma 自身可以连续滚动。

## 1.5 第一次工程版完成，但产品验证严重不足

窗口完成了：

- 私有站点注册表；
- 站点级广告黑名单 / DOM 清理；
- 多个 Pure BookSource；
- 河图账号池 / 自动签到；
- 私人站点入口；
- CI；
- Debug APK。

第一次 APK 对应旧基线附近：

~~~text
8df3b33c670428fedea54c48de991b294e1ecdc0
~~~

工程上：

~~~text
JSON OK
unit tests OK
assembleAppDebug OK
artifact OK
~~~

但真机一跑，暴露出“工程通过”与“真实可用”之间巨大差距。

## 1.6 真机反馈把项目从“功能搭建”推到“逐站闭环”

用户真机反馈是本窗口真正的转折点：

- 私人站点藏在“我的”不直觉；
- 搜索覆盖严重不足；
- TWKAN 简繁处理仍错；
- 第一版主 / 八叉打不开；
- UAA 登录失败；
- 河图能登录但书打不开；
- 69 验证循环；
- 视频站广告仍多；
- MissAV 会直接跳站；
- 河图账号池签到没有工作，而且产品价值不值得复杂度。

由此冻结：

> **CI 绿不再是“完成”证据，只是最低工程门槛。**

---

# 2. 末轮返工与当前仓库现实

旧 APK 后，`private-site-platform` 又前进 23 个提交。

恢复结算时的代码头：

~~~text
ee3d1e8ab18ee75709586a9ad9f55c5155327760
private: block known ad top-level redirects before leaving managed sites
~~~

相对旧 APK 基线 `8df3b33...` 的主要实际变化：

~~~text
UI
- 第二栏加入私人站点一层入口
- “我的”里的私人站点入口删除

河图
- 删除整个账号池 / 自动签到 / Job / Receiver / secret store
- 回归单账号正常登录
- 修正详情 / 目录 selector

TWKAN
- 原关键词 + 简转繁关键词双搜索
- 搜索 / 详情 / 目录 / 正文显式转简

69
- 删除全局搜索强制浏览器验证
- Cloudflare 时安静失败

八叉
- 重写当前站点结构与入口

WebView
- Cookie 不再全局 session 清空
- 按站点隔离
- 开启第三方 Cookie 支持现代认证

广告
- 扩展站点广告域 / selector
- 顶层导航广告跳转原生拦截
- MissAV 跳站重点防护
~~~

---

# 3. Repository Drift Revalidation

用户终端交接消息中有一句：

> “这一轮第一版主/UAA 还没彻底收完，也还没重新跑最终 CI。”

恢复时 GitHub 已经继续前进，因此必须按 Repository Reality 修正：

~~~text
branch:
private-site-platform

head:
ee3d1e8ab18ee75709586a9ad9f55c5155327760

Private CI:
run #27
run_id 34598125581
completed / success

结论：
“还没重新跑 CI”
= 终端消息发出时或生成时的暂态描述
= 当前已过时
~~~

但：

~~~text
CI SUCCESS
!= UAA 真机登录 PASS
!= 第一版主原生阅读 PASS
!= 广告清理 PASS
!= 搜索覆盖 PASS
~~~

这层不能混淆。

---

# 4. 当前文件级 live state

新窗口恢复时建议优先查看：

## 4.1 UI / 一层入口

- `app/src/main/java/io/legado/app/ui/main/explore/ExploreFragment.kt`
- `app/src/main/res/layout/fragment_explore.xml`

当前含：

~~~text
搜书 Chip
+
PrivateSiteRegistry 全部站点 Chip
+
下面原发现源列表
~~~

## 4.2 站点身份 / 广告

- `app/src/main/java/io/legado/app/help/site/PrivateSiteRegistry.kt`
- `app/src/main/java/io/legado/app/help/site/PrivateSiteCleaner.kt`
- `app/src/main/java/io/legado/app/ui/browser/WebViewActivity.kt`
- `app/src/main/java/io/legado/app/help/webView/WebViewPool.kt`

## 4.3 关键文字站

- `app/src/main/assets/privateSites/bookSources/twkan-pure.json`
- `app/src/main/assets/privateSites/bookSources/69shuba-pure.json`
- `app/src/main/assets/privateSites/bookSources/bachashuku-pure.json`
- `app/src/main/assets/privateSites/bookSources/diyibanzhu-pure.json`
- `app/src/main/assets/privateSites/bookSources/uaa-pure.json`
- `app/src/main/assets/privateSites/bookSources/hotupub-pure.json`
- `app/src/main/assets/privateSites/bookSources/uukanshu-pure.json`
- `app/src/main/assets/privateSites/bookSources/quanben-io-pure.json`
- `app/src/main/assets/privateSites/bookSources/cool18-pure.json`

## 4.4 安装 / CI

- `app/src/main/java/io/legado/app/help/site/PrivateBookSourceInstaller.kt`
- `.github/workflows/private-ci.yml`

---

# 5. 当前最重要的未闭合点

## 5.1 第一版主：仓库还没执行末轮裁决

当前文件仍然：

~~~text
bookSourceName = 第一版主 Pure
bookSourceUrl = https://www.111bz.cc/?penrix_builtin=diyibanzhu
enabled = true
ruleContent = id.content@text
~~~

但窗口末轮已判断：

- 当前第一版主真实正文存在字符占位 / 解码问题；
- 普通 selector 不能假装解决；
- 没拿到可靠解码规则时，应该先禁用原生源，只保留网站入口。

所以第一版主是**恢复后最先需要闭合的 Repository Gap**。

## 5.2 UAA：代码修复存在，真实登录未复验

当前 UAA Pure 仍使用：

- 官方小说 API 搜索；
- 官方网页登录；
- CookieJar；
- HTML 详情 / 目录 / 正文。

系统 Cookie 隔离和第三方 Cookie 已修改，但：

> **用户真实账号在新修复后的 APK 上还没有重新验证。**

## 5.3 搜索覆盖：规则需要冷门书压测

真实用户反馈已经证明：

~~~text
同一站网站搜索可找到
!= App 书源搜索能找到
~~~

必须继续用冷门书压：

- 《大时代1958》
- 《峨眉剑仙》

并观察每条线路独立结果。

## 5.4 广告：需要真机而不是代码审查

用户已经明确看到：

- Cool18 广告；
- Hanime1 广告；
- Pornhub 广告；
- XVideos 广告；
- MissAV 跳站。

最新广告规则与原生跳转拦截已经写入，但必须在手机上重新跑。

---

# 6. 当前不应重新打开的旧路线

以下已经关闭：

~~~text
河图账号池 / 自动签到
社区精选源产品层
私人站点放“我的”作为主入口
新增底部私人 / 视频 Tab
把发现源当私人版主导航
69 全局搜索强制人机验证
先继续扩站再修已有站
~~~

旧 Draft PR #1 / #2 只作形成史，不是 current branch。

---

# 7. 新窗口推荐读取顺序

第一屏：

1. `WHOLE-WINDOW-HANDOFF-CHECKPOINT-001.md`
2. `WINDOW-RECOVERY-MANIFEST-001.md`
3. `SESSION-EVIDENCE-001.md`

随后 revalidate：

4. `private-site-platform` 当前 branch head
5. 最近 Private CI
6. `diyibanzhu-pure.json`
7. `twkan-pure.json`
8. `uaa-pure.json`
9. `hotupub-pure.json`
10. `PrivateSiteRegistry.kt`
11. `PrivateSiteCleaner.kt`
12. `WebViewActivity.kt` / `WebViewPool.kt`
13. `ExploreFragment.kt`

只有遇到形成史问题时，再看：

- PR #1 TWKAN 原型；
- PR #2 视频站原型；
- 旧 `8df3b33...` 测试 APK 对应代码。

不要 Fresh Onboarding 整个 Sigma。

---

# 8. 下一窗口最小启动提示

可直接使用：

~~~text
这是 Penrix/legado-E 私人 Sigma 项目的 Continuation Recovery，不是 Fresh Onboarding。

使用 private-site-platform 当前 live state。

先读：
1. working/session-continuity/windows/WIN-20260911-SIGMA-PRIVATE-001/WHOLE-WINDOW-HANDOFF-CHECKPOINT-001.md
2. 同目录 WINDOW-RECOVERY-MANIFEST-001.md
3. 同目录 SESSION-EVIDENCE-001.md

然后重新读取当前 branch head 与 CI，仓库现实优先于恢复包中的旧 SHA。

恢复后不要扩新站，不要恢复河图账号池，不要重做底部导航。

第一优先级：闭合第一版主“可靠解码 or 暂时禁用原生源”的仓库缺口；随后产出新 APK，用《大时代1958》《峨眉剑仙》以及 UAA / 八叉 / 河图 / Cool18 / Hanime1 / Pornhub / XVideos / MissAV 做真机回归。
~~~

---

# 9. 已知恢复缺口

~~~text
source_conversation_id:
UNKNOWN / NOT EXPOSED

full_message_forensics:
NOT PERFORMED

semantic_consequence:
LOW

reason:
用户已经提供终端进度包；
当前仓库能验证关键返工和最新 CI；
本次目标是恢复可继续施工状态，不是逐条聊天取证。
~~~

最终判断：

> **语义恢复充分；当前主线不是继续搭平台，而是把已经选中的站点按真机反馈逐条调通。**
