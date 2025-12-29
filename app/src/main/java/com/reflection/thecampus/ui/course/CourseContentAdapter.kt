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

            // Lock state - unlock if enrolled OR content is public OR content is free
            val isLocked = !isEnrolled && !item.isPublic && !item.isFree
            val isFree = item.isPublic || item.isFree
            
            timber.log.Timber.d("CourseContent FOLDER: ${item.name}, isEnrolled=$isEnrolled, isPublic=${item.isPublic}, isFree=${item.isFree}, isLocked=$isLocked")
            
            // Show FREE badge for free content
            val freeBadge = binding.root.findViewById<android.widget.TextView>(com.reflection.thecampus.R.id.tvFreeBadge)
            freeBadge.visibility = if (isFree && !isEnrolled) View.VISIBLE else View.GONE
            
            // Apply gold background for free content
            val cardView = binding.root.findViewById<androidx.cardview.widget.CardView>(com.reflection.thecampus.R.id.cardFolder)
            if (isFree && !isEnrolled) {
                cardView.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                cardView.setBackgroundResource(com.reflection.thecampus.R.drawable.bg_free_content_light)
            } else {
                cardView.background = null
                val typedValue = android.util.TypedValue()
                binding.root.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
                cardView.setCardBackgroundColor(typedValue.data)
            }
            
            binding.ivLock.visibility = if (isLocked) View.VISIBLE else View.GONE
            binding.tvFolderName.alpha = if (isLocked) 0.6f else 1.0f

            // Handle children
            if (node.children.isNotEmpty()) {
                childAdapter.submitList(node.children)
                binding.ivArrow.visibility = View.VISIBLE
                
                binding.headerLayout.setOnClickListener {
                    // Allow expansion if user is enrolled OR content is free
                    if (isLocked) {
                        // Shake animation for locked content
                        val shakeAnim = android.view.animation.AnimationUtils.loadAnimation(binding.root.context, com.reflection.thecampus.R.anim.shake_lock)
                        binding.root.startAnimation(shakeAnim)
                        binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
                        // Show toast for locked content
                        onFolderClick(item)
                    } else {
                        // Allow expansion for enrolled users or free content
                        binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
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
            
            // Simply toggle visibility without animations on RecyclerView
            // (animations were causing rendering issues with nested cards)
            if (isExpanded) {
                // Ensure adapter and layout manager are properly set
                if (binding.rvSubContent.adapter == null) {
                    binding.rvSubContent.layoutManager = LinearLayoutManager(binding.root.context)
                    binding.rvSubContent.adapter = childAdapter
                }
                // Reset height to WRAP_CONTENT when expanding
                val params = binding.rvSubContent.layoutParams
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                binding.rvSubContent.layoutParams = params
                binding.rvSubContent.visibility = View.VISIBLE
            } else {
                binding.rvSubContent.visibility = View.GONE
                // Force height to 0 when collapsed
                val params = binding.rvSubContent.layoutParams
                params.height = 0
                binding.rvSubContent.layoutParams = params
            }
            
            // Request layout to prevent overlap issues
            binding.root.requestLayout()
            
            // Post another layout request to ensure proper recalculation
            binding.root.post {
                binding.root.requestLayout()
            }
            
            // Smooth arrow rotation (this animation works fine)
            binding.ivArrow.animate()
                .rotation(if (isExpanded) 90f else 0f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
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

            // Lock state - unlock if enrolled OR content is public OR content is free
            val isLocked = !isEnrolled && !item.isPublic && !item.isFree
            val isFree = item.isPublic || item.isFree
            
            timber.log.Timber.d("CourseContent FILE: ${item.name}, isEnrolled=$isEnrolled, isPublic=${item.isPublic}, isFree=${item.isFree}, isLocked=$isLocked")
            
            // Show FREE badge for free content
            val freeBadge = binding.root.findViewById<android.widget.TextView>(com.reflection.thecampus.R.id.tvFreeBadge)
            freeBadge.visibility = if (isFree && !isEnrolled) View.VISIBLE else View.GONE
            
            // Apply gold background for free content
            val cardView = binding.root.findViewById<androidx.cardview.widget.CardView>(com.reflection.thecampus.R.id.cardFile)
            if (isFree && !isEnrolled) {
                cardView.setCardBackgroundColor(android.graphics.Color.TRANSPARENT)
                cardView.setBackgroundResource(com.reflection.thecampus.R.drawable.bg_free_content_light)
            } else {
                cardView.background = null
                val typedValue = android.util.TypedValue()
                binding.root.context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
                cardView.setCardBackgroundColor(typedValue.data)
            }
            
            binding.ivLock.visibility = if (isLocked) View.VISIBLE else View.GONE
            binding.tvFileName.alpha = if (isLocked) 0.6f else 1.0f

            // Allow file click - the fragment will handle access control
            binding.root.setOnClickListener {
                if (isLocked) {
                    // Shake animation for locked content
                    val shakeAnim = android.view.animation.AnimationUtils.loadAnimation(binding.root.context, com.reflection.thecampus.R.anim.shake_lock)
                    binding.root.startAnimation(shakeAnim)
                    binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.REJECT)
                } else {
                    binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                }
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
