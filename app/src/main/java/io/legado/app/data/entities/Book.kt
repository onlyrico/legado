package io.legado.app.data.entities

import android.os.Parcelable
import android.text.TextUtils.isEmpty
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import io.legado.app.constant.BookType
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.splitNotBlank
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlin.math.max

@Parcelize
@Entity(tableName = "books", indices = [(Index(value = ["bookUrl"], unique = true))])
data class Book(
    @PrimaryKey
    var bookUrl: String = "",                   // 详情页Url(本地书源存储完整文件路径)
    var tocUrl: String? = null,                    // 目录页Url (toc=table of Contents)
    var origin: String = BookType.local,        // 书源URL(默认BookType.local)
    var originName: String = "",                //书源名称
    var name: String? = null,                   // 书籍名称(书源获取)
    var author: String? = null,                 // 作者名称(书源获取)
    var kind: String? = null,                    // 分类信息(书源获取)
    var customTag: String? = null,              // 分类信息(用户修改)
    var coverUrl: String? = null,               // 封面Url(书源获取)
    var customCoverUrl: String? = null,         // 封面Url(用户修改)
    var intro: String? = null,            // 简介内容(书源获取)
    var customIntro: String? = null,      // 简介内容(用户修改)
    var charset: String? = null,                // 自定义字符集名称(仅适用于本地书籍)
    var type: Int = 0,                          // @BookType
    var group: Int = 0,                         // 自定义分组索引号
    var latestChapterTitle: String? = null,     // 最新章节标题
    var latestChapterTime: Long = 0,            // 最新章节标题更新时间
    var lastCheckTime: Long = 0,                // 最近一次更新书籍信息的时间
    var lastCheckCount: Int = 0,                // 最近一次发现新章节的数量
    var totalChapterNum: Int = 0,               // 书籍目录总数
    var durChapterTitle: String? = null,        // 当前章节名称
    var durChapterIndex: Int = 0,               // 当前章节索引
    var durChapterPos: Int = 0,                 // 当前阅读的进度(首行字符的索引位置)
    var durChapterTime: Long = 0,               // 最近一次阅读书籍的时间(打开正文的时间)
    var wordCount: String? = null,
    var canUpdate: Boolean = true,              // 刷新书架时更新书籍信息
    var order: Int = 0,                         // 手动排序
    var useReplaceRule: Boolean = true,         // 正文使用净化替换规则
    var variable: String? = null                // 自定义书籍变量信息(用于书源规则检索书籍信息)
) : Parcelable, BaseBook {
    @IgnoredOnParcel
    @Ignore
    override var variableMap: HashMap<String, String>? = null
        get() = run {
            initVariableMap()
            return field
        }

    fun getUnreadChapterNum() = max(totalChapterNum - durChapterIndex - 1, 0)

    fun getDisplayCover() = if (customCoverUrl.isNullOrEmpty()) coverUrl else customCoverUrl

    fun getDisplayIntro() = if (customIntro.isNullOrEmpty()) intro else customIntro

    private fun initVariableMap() {
        if (variableMap == null) {
            variableMap = if (isEmpty(variable)) {
                HashMap()
            } else {
                GSON.fromJsonObject<HashMap<String, String>>(variable)
            }
        }
    }

    override fun putVariable(key: String, value: String) {
        initVariableMap()
        variableMap?.put(key, value)
        variable = GSON.toJson(variableMap)
    }

    fun getKindList(): List<String> {
        val kindList = arrayListOf<String>()
        wordCount?.let {
            if (it.isNotBlank()) kindList.add(it)
        }
        kind?.let {
            val kinds = it.splitNotBlank(",", "\n")
            kindList.addAll(kinds)
        }
        return kindList
    }
}