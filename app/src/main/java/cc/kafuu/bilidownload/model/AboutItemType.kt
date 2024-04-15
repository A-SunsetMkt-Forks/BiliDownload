package cc.kafuu.bilidownload.model

import androidx.annotation.IntDef


@IntDef(AboutItemType.ITEM_INVITATION, AboutItemType.ITEM_VERSION, AboutItemType.ITEM_LICENSE,
    AboutItemType.ITEM_GRADE, AboutItemType.ITEM_US)
@Retention(
    AnnotationRetention.SOURCE
)
annotation class AboutItemType {
    companion object {
        const val ITEM_INVITATION = 0
        const val ITEM_VERSION = 1
        const val ITEM_LICENSE = 2
        const val ITEM_GRADE = 3
        const val ITEM_US = 4
    }
}