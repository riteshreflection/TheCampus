package com.reflection.thecampus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class CourseClassesFragment : Fragment() {

    private var classIds: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            classIds = it.getStringArrayList(ARG_CLASS_IDS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_course_classes, container, false)
        
        val layoutEmpty = view.findViewById<View>(R.id.layoutEmpty)
        
        if (classIds.isNullOrEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
        } else {
            layoutEmpty.visibility = View.GONE
            // TODO: Load and display classes
        }
        
        return view
    }

    companion object {
        private const val ARG_CLASS_IDS = "class_ids"

        fun newInstance(classIds: List<String>) =
            CourseClassesFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_CLASS_IDS, ArrayList(classIds))
                }
            }
    }
}
