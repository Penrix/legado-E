package io.legado.app.ui.main.my

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.FragmentMyConfigBinding
import io.legado.app.help.config.ThemeConfig
import io.legado.app.help.site.PrivateSiteKind
import io.legado.app.help.site.PrivateSiteRegistry
import io.legado.app.help.site.hotupub.HotuAccountPool
import io.legado.app.help.site.hotupub.HotuAutoSignIn
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.prefs.NameListPreference
import io.legado.app.lib.prefs.SwitchPreference
import io.legado.app.lib.prefs.fragment.PreferenceFragment
import io.legado.app.lib.theme.primaryColor
import io.legado.app.service.WebService
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.ReadRecordActivity
import io.legado.app.ui.book.bookmark.AllBookmarkActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.book.toc.rule.TxtTocRuleActivity
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigTag
import io.legado.app.ui.dict.rule.DictRuleActivity
import io.legado.app.ui.file.FileManageActivity
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.replace.ReplaceRuleActivity
import io.legado.app.utils.LogUtils
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.observeEventSticky
import io.legado.app.utils.openUrl
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.showHelp
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

class MyFragment() : BaseFragment(R.layout.fragment_my_config), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentMyConfigBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        val fragmentTag = "prefFragment"
        var preferenceFragment = childFragmentManager.findFragmentByTag(fragmentTag)
        if (preferenceFragment == null) preferenceFragment = MyPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.pre_fragment, preferenceFragment, fragmentTag).commit()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_my, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_help -> showHelp("appHelp")
        }
    }

    /**
     * 配置
     */
    class MyPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            putPrefBoolean(PreferKey.webService, WebService.isRun)
            addPreferencesFromResource(R.xml.pref_main)
            findPreference<SwitchPreference>("webService")?.onLongClick {
                if (!WebService.isRun) {
                    return@onLongClick false
                }
                context?.selector(arrayListOf("复制地址", "浏览器打开")) { _, i ->
                    when (i) {
                        0 -> context?.sendToClip(it.summary.toString())
                        1 -> context?.openUrl(it.summary.toString())
                    }
                }
                true
            }
            observeEventSticky<String>(EventBus.WEB_SERVICE) {
                findPreference<SwitchPreference>(PreferKey.webService)?.let {
                    it.isChecked = WebService.isRun
                    it.summary = if (WebService.isRun) {
                        WebService.hostAddress
                    } else {
                        getString(R.string.web_service_desc)
                    }
                }
            }
            findPreference<NameListPreference>(PreferKey.themeMode)?.let {
                it.setOnPreferenceChangeListener { _, _ ->
                    view?.post { ThemeConfig.applyDayNight(requireContext()) }
                    true
                }
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.webService -> {
                    if (requireContext().getPrefBoolean("webService")) {
                        WebService.start(requireContext())
                    } else {
                        WebService.stop(requireContext())
                    }
                }

                "recordLog" -> LogUtils.upLevel()
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "bookSourceManage" -> startActivity<BookSourceActivity>()
                "privateSites" -> openPrivateSites()
                "hotuAccountPool" -> openHotuAccountPool()
                "replaceManage" -> startActivity<ReplaceRuleActivity>()
                "dictRuleManage" -> startActivity<DictRuleActivity>()
                "txtTocRuleManage" -> startActivity<TxtTocRuleActivity>()
                "bookmark" -> startActivity<AllBookmarkActivity>()
                "setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.OTHER_CONFIG)
                }

                "web_dav_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.BACKUP_CONFIG)
                }

                "theme_setting" -> startActivity<ConfigActivity> {
                    putExtra("configTag", ConfigTag.THEME_CONFIG)
                }

                "fileManage" -> startActivity<FileManageActivity>()
                "readRecord" -> startActivity<ReadRecordActivity>()
                "about" -> startActivity<AboutActivity>()
                "exit" -> activity?.finish()
            }
            return super.onPreferenceTreeClick(preference)
        }

        private fun openPrivateSites() {
            val sites = PrivateSiteRegistry.profiles
            val labels = ArrayList(sites.map { site ->
                val kind = when (site.kind) {
                    PrivateSiteKind.NOVEL -> "小说"
                    PrivateSiteKind.FORUM -> "论坛"
                    PrivateSiteKind.VIDEO -> "视频"
                    PrivateSiteKind.NAVIGATION -> "导航"
                }
                "$kind · ${site.displayName}"
            })
            context?.selector(labels) { _, index ->
                val site = sites[index]
                startActivity<WebViewActivity> {
                    putExtra("url", site.startUrl)
                    putExtra("title", site.displayName)
                    putExtra("sourceName", "Penrix 私人站点")
                }
            }
        }

        private fun openHotuAccountPool() {
            val accounts = HotuAccountPool.accounts()
            val activeId = HotuAccountPool.activeAccount()?.id
            val labels = arrayListOf(
                "打开河图登录页",
                "保存当前河图登录为账号",
                "立即签到全部账号"
            )
            accounts.forEach { account ->
                val active = if (account.id == activeId) "✓ " else ""
                val status = when (account.lastSignStatus) {
                    HotuAccountPool.SignStatus.NEVER -> "未签到"
                    HotuAccountPool.SignStatus.SUCCESS -> "已签到"
                    HotuAccountPool.SignStatus.ALREADY -> "今日已签"
                    HotuAccountPool.SignStatus.EXPIRED -> "登录失效"
                    HotuAccountPool.SignStatus.UNSUPPORTED -> "规则待更新"
                    HotuAccountPool.SignStatus.FAILED -> "签到失败"
                }
                labels += "$active${account.label} · $status"
            }

            context?.selector(labels) { _, index ->
                when (index) {
                    0 -> {
                        startActivity<WebViewActivity> {
                            putExtra("url", "https://www.hotupub.net/Login/Index")
                            putExtra("title", "河图账号登录")
                            putExtra("sourceName", "登录完成后返回账号池并保存当前登录")
                        }
                    }

                    1 -> {
                        val saved = HotuAccountPool.captureCurrentLogin()
                        if (saved == null) {
                            toast("当前没有可保存的河图登录态，请先打开登录页完成登录")
                        } else {
                            toast("已保存并切换到 ${saved.label}")
                        }
                    }

                    2 -> {
                        toast("开始签到 ${accounts.size} 个河图账号")
                        HotuAutoSignIn.runDueAsync(force = true) { results ->
                            val success = results.count {
                                it.result.status == HotuAccountPool.SignStatus.SUCCESS ||
                                    it.result.status == HotuAccountPool.SignStatus.ALREADY
                            }
                            toast("河图签到完成：$success/${results.size}")
                        }
                    }

                    else -> manageHotuAccount(accounts[index - 3])
                }
            }
        }

        private fun manageHotuAccount(account: HotuAccountPool.Account) {
            val actions = arrayListOf(
                "设为当前阅读账号",
                "立即签到这个账号",
                if (account.enabled) "暂停自动签到" else "恢复自动签到",
                "删除账号"
            )
            context?.selector(actions) { _, index ->
                when (index) {
                    0 -> {
                        if (HotuAccountPool.setActive(account.id)) {
                            toast("当前河图账号：${account.label}")
                        } else {
                            toast("切换失败：账号 Cookie 不可用")
                        }
                    }

                    1 -> {
                        toast("正在签到 ${account.label}")
                        HotuAutoSignIn.runAccountAsync(account.id) { result ->
                            toast(
                                result?.let { "${account.label}：${it.result.message}" }
                                    ?: "账号不存在"
                            )
                        }
                    }

                    2 -> {
                        HotuAccountPool.setEnabled(account.id, !account.enabled)
                        toast(if (account.enabled) "已暂停自动签到" else "已恢复自动签到")
                    }

                    3 -> {
                        HotuAccountPool.remove(account.id)
                        toast("已删除 ${account.label}")
                    }
                }
            }
        }

        private fun toast(message: String) {
            context?.let {
                Toast.makeText(it, message, Toast.LENGTH_SHORT).show()
            }
        }

    }
}