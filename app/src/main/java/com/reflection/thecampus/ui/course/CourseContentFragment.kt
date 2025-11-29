package com.reflection.thecampus.ui.course

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.Course
import com.reflection.thecampus.CourseContentItem
import com.reflection.thecampus.R

class CourseContentFragment : Fragment() {

    private var course: Course? = null
    private var isEnrolled: Boolean = false
    private lateinit var adapter: CourseContentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            course = it.getParcelable(ARG_COURSE)
            isEnrolled = it.getBoolean(ARG_IS_ENROLLED, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_course_content, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView(view)
        updateList()
    }

    private fun setupRecyclerView(view: View) {
        val rvContent = view.findViewById<RecyclerView>(R.id.rvContent)
        rvContent.layoutManager = LinearLayoutManager(context)
        
        adapter = CourseContentAdapter(
            isEnrolled = isEnrolled,
            onFolderClick = { item ->
                // Only called for locked items or empty folders if we want
                if (!isEnrolled && !item.isPublic) {
                    Toast.makeText(context, "Enroll to view content", Toast.LENGTH_SHORT).show()
                }
            },
            onFileClick = { item ->
                openFile(item)
            }
        )
        rvContent.adapter = adapter
    }

    private fun openFile(item: CourseContentItem) {
        if (!isEnrolled && !item.isPublic) {
            Toast.makeText(context, "Enroll to view content", Toast.LENGTH_SHORT).show()
            return
        }

        if (item.url.isNotEmpty()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open file", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Invalid file URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateList() {
        val contentMap = course?.content ?: emptyMap()
        val view = view ?: return
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        if (contentMap.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            adapter.submitList(emptyList())
            return
        }
        tvEmpty.visibility = View.GONE

        val tree = buildTree(contentMap)
        adapter.submitList(tree)
    }

    private fun buildTree(contentMap: Map<String, CourseContentItem>): List<ContentNode> {
        // 1. Find all root items
        val rootItems = contentMap.values
            .filter { it.parentId.isNullOrEmpty() && it.status == "published" }
            .sortedBy { it.name }

        // 2. Recursively build nodes
        return rootItems.map { item ->
            buildNode(item, contentMap)
        }
    }

    private fun buildNode(item: CourseContentItem, contentMap: Map<String, CourseContentItem>): ContentNode {
        // Find children of this item
        val childrenItems = contentMap.values
            .filter { it.parentId == item.id && it.status == "published" }
            .sortedBy { it.name }
        
        val childrenNodes = childrenItems.map { child ->
            buildNode(child, contentMap)
        }

        return ContentNode(item, childrenNodes)
    }

    companion object {
        private const val ARG_COURSE = "course"
        private const val ARG_IS_ENROLLED = "is_enrolled"

        fun newInstance(course: Course, isEnrolled: Boolean) =
            CourseContentFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_COURSE, course)
                    putBoolean(ARG_IS_ENROLLED, isEnrolled)
                }
            }
    }
}
