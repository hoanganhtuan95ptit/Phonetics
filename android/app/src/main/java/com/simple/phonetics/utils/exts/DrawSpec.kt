package com.simple.phonetics.utils.exts

import com.simple.ui.precompute.DrawSpec
import com.simple.ui.precompute.node.GroupSpec

/**
 * Tìm [DrawSpec] con đầu tiên có `node.id == targetId` trong subtree bắt đầu từ [this].
 *
 * Duyệt depth-first theo thứ tự vẽ (children order). Trả về `this` nếu chính spec
 * này khớp id.
 *
 * Lưu ý:
 *  - [com.simple.ui.precompute.node.LayoutNode.id] mặc định `null`. Muốn look up
 *    được, phải gán id khi build node (vd `TextNode(id = "phonetic", text = ...)`).
 *  - Chỉ đệ quy vào container **[GroupSpec]** (kết quả của LinearNode / ConstraintNode /
 *    FlexboxNode). `SizedSpec` là internal của node-engine → không đi xuyên qua được
 *    ở lớp app; nhưng `SizedSpec.node` trỏ về node của child nên id vẫn match ở lớp
 *    bọc ngoài. Nếu subtree chứa custom container khác, thêm case tương ứng.
 */
fun DrawSpec.findById(targetId: Any): DrawSpec? {
    if (node?.id == targetId) return this
    return when (this) {
        is GroupSpec -> children.firstNotNullOfOrNull { it.findById(targetId) }
        else -> null
    }
}

/**
 * Ép kiểu tiện lợi: tìm spec theo id và cast thành [T].
 * Trả về `null` nếu không tìm thấy hoặc không cast được.
 */
inline fun <reified T : DrawSpec> DrawSpec.findByIdAs(targetId: Any): T? =
    findById(targetId) as? T

/**
 * Duyệt mọi spec trong subtree (bao gồm [this]) theo thứ tự depth-first.
 * Hữu ích khi cần thao tác trên nhiều spec cùng lúc.
 */
fun DrawSpec.walk(action: (DrawSpec) -> Unit) {
    action(this)
    if (this is GroupSpec) children.forEach { it.walk(action) }
}
