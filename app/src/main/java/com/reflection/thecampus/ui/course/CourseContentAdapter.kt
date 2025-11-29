package com.reflection.thecampus.ui.course

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.CourseContentItem
import com.reflection.thecampus.R
import com.reflection.thecampus.databinding.ItemCourseContentFileBinding
import com.reflection.thecampus.databinding.ItemCourseContentFolderBinding

class CourseContentAdapter(
    private val isEnrolled: Boolean,
    private val onFolderClick: (CourseContentItem) -> Unit,
    private val onFileClick: (CourseContentItem) -> Unit,
    private val isRoot: Boolean = true // New parameter to track nesting level
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ContentNode> = emptyList()

    fun submitList(newItems: List<ContentNode>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].item.type == "folder") TYPE_FOLDER else TYPE_FILE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FOLDER) {
            val binding = ItemCourseContentFolderBinding.inflate(inflater, parent, false)
            FolderViewHolder(binding)
        } else {
            val binding = ItemCourseContentFileBinding.inflate(inflater, parent, false)
            FileViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val node = items[position]
        val isLast = position == items.size - 1
        
        if (holder is FolderViewHolder) {
            holder.bind(node, isLast)
        } else if (holder is FileViewHolder) {
            holder.bind(node, isLast)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class FolderViewHolder(private val binding: ItemCourseContentFolderBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        // Child adapter for nested content - pass isRoot = false
        private val childAdapter = CourseContentAdapter(isEnrolled, onFolderClick, onFileClick, isRoot = false)
        private var isExpanded = false

        init {
            binding.rvSubContent.layoutManager = LinearLayoutManager(binding.root.context)
            binding.rvSubContent.adapter = childAdapter
        }

        fun bind(node: ContentNode, isLast: Boolean) {
            val item = node.item
            binding.tvFolderName.text = item.name
            
            // Tree Lines Logic
            if (isRoot) {
                binding.ivTreeLines.visibility = View.GONE
            } else {
                binding.ivTreeLines.visibility = View.VISIBLE
                val drawableRes = if (isLast) R.drawable.ic_tree_last else R.drawable.ic_tree_branch
                binding.ivTreeLines.setImageResource(drawableRes)
            }

            // Lock state
            val isLocked = !isEnrolled && !item.isPublic
            binding.ivLock.visibility = if (isLocked) View.VISIBLE else View.GONE
            binding.tvFolderName.alpha = if (isLocked) 0.6f else 1.0f

            // Handle children
            if (node.children.isNotEmpty()) {
                childAdapter.submitList(node.children)
                binding.ivArrow.visibility = View.VISIBLE
                
                binding.headerLayout.setOnClickListener {
                    if (isLocked) {
                        onFolderClick(item)
                    } else {
                        toggleExpansion()
                    }
                }
            } else {
                binding.ivArrow.visibility = View.INVISIBLE
                binding.headerLayout.setOnClickListener(null)
            }
        }

        private fun toggleExpansion() {
            isExpanded = !isExpanded
            binding.rvSubContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.ivArrow.animate().rotation(if (isExpanded) 90f else 0f).setDuration(200).start()
        }
    }

    inner class FileViewHolder(private val binding: ItemCourseContentFileBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(node: ContentNode, isLast: Boolean) {
            val item = node.item
            binding.tvFileName.text = item.name

            // Tree Lines Logic
            if (isRoot) {
                binding.ivTreeLines.visibility = View.GONE
            } else {
                binding.ivTreeLines.visibility = View.VISIBLE
                val drawableRes = if (isLast) R.drawable.ic_tree_last else R.drawable.ic_tree_branch
                binding.ivTreeLines.setImageResource(drawableRes)
            }

            // Lock state
            val isLocked = !isEnrolled && !item.isPublic
            binding.ivLock.visibility = if (isLocked) View.VISIBLE else View.GONE
            binding.tvFileName.alpha = if (isLocked) 0.6f else 1.0f

            binding.root.setOnClickListener {
                onFileClick(item)
            }
        }
    }

    companion object {
        private const val TYPE_FOLDER = 0
        private const val TYPE_FILE = 1
    }
}

data class ContentNode(
    val item: CourseContentItem,
    val children: List<ContentNode> = emptyList()
)
