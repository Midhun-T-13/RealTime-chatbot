package com.example.realtimechatapplicaition.ui.chatlist

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.realtimechatapplicaition.R

class SwipeToDeleteCallback(
    private val context: Context,
    private val onSwipe: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
    private val intrinsicWidth = deleteIcon?.intrinsicWidth ?: 0
    private val intrinsicHeight = deleteIcon?.intrinsicHeight ?: 0
    private val background = ColorDrawable()
    private val backgroundColor = ContextCompat.getColor(context, R.color.delete_background)
    private val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwipe(viewHolder.bindingAdapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val itemHeight = itemView.bottom - itemView.top
        val isCanceled = dX == 0f && !isCurrentlyActive

        if (isCanceled) {
            clearCanvas(c, itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            return
        }

        // Draw background
        background.color = backgroundColor

        if (dX > 0) {
            // Swiping to the right
            background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
        } else {
            // Swiping to the left
            background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        }
        background.draw(c)

        // Calculate position of delete icon
        val iconTop = itemView.top + (itemHeight - intrinsicHeight) / 2
        val iconMargin = (itemHeight - intrinsicHeight) / 2
        val iconBottom = iconTop + intrinsicHeight

        if (dX > 0) {
            // Swiping to the right - icon on left side
            val iconLeft = itemView.left + iconMargin
            val iconRight = itemView.left + iconMargin + intrinsicWidth
            deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        } else {
            // Swiping to the left - icon on right side
            val iconLeft = itemView.right - iconMargin - intrinsicWidth
            val iconRight = itemView.right - iconMargin
            deleteIcon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
        }

        deleteIcon?.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    private fun clearCanvas(c: Canvas?, left: Float, top: Float, right: Float, bottom: Float) {
        c?.drawRect(left, top, right, bottom, clearPaint)
    }
}
