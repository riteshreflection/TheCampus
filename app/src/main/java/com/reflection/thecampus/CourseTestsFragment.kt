package com.reflection.thecampus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.reflection.thecampus.ui.test.TestInterfaceActivity

class CourseTestsFragment : Fragment() {

    private var linkedTests: ArrayList<String>? = null
    private lateinit var rvTests: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvNoTests: TextView
    private val testsList = mutableListOf<TestSummary>()
    private lateinit var adapter: TestAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            linkedTests = it.getStringArrayList(ARG_LINKED_TESTS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_course_tests, container, false)
        
        rvTests = view.findViewById(R.id.rvTests)
        progressBar = view.findViewById(R.id.progressBar)
        tvNoTests = view.findViewById(R.id.tvNoTests)

        setupRecyclerView()
        loadTests()

        return view
    }

    private fun setupRecyclerView() {
        // Default to false if not provided, though it should be provided
        val isEnrolled = arguments?.getBoolean(ARG_IS_ENROLLED) ?: false
        
        adapter = TestAdapter(testsList, isEnrolled) { test ->
            // Handle attempt click
            val intent = android.content.Intent(context, TestInterfaceActivity::class.java)
            intent.putExtra("TEST_ID", test.id)
            startActivity(intent)
        }
        rvTests.layoutManager = LinearLayoutManager(context)
        rvTests.adapter = adapter
    }

    private fun loadTests() {
        if (linkedTests.isNullOrEmpty()) {
            showEmptyState()
            return
        }

        progressBar.visibility = View.VISIBLE
        tvNoTests.visibility = View.GONE
        rvTests.visibility = View.GONE

        val database = FirebaseDatabase.getInstance()
        val testsRef = database.getReference("tests")
        
        var loadedCount = 0
        val totalTests = linkedTests!!.size
        testsList.clear()

        for (testId in linkedTests!!) {
            testsRef.child(testId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val test = snapshot.getValue(TestSummary::class.java)
                        if (test != null && test.status == "published") {
                            test.id = snapshot.key ?: ""
                            testsList.add(test)
                        }
                        loadedCount++
                        
                        if (loadedCount == totalTests) {
                            onTestsLoaded()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseDebug", "Error deserializing test $testId", e)
                        loadedCount++
                        if (loadedCount == totalTests) {
                            onTestsLoaded()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    if (loadedCount == totalTests) {
                        onTestsLoaded()
                    }
                }
            })
        }
    }

    private fun onTestsLoaded() {
        progressBar.visibility = View.GONE
        if (testsList.isEmpty()) {
            showEmptyState()
        } else {
            rvTests.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        rvTests.visibility = View.GONE
        tvNoTests.visibility = View.VISIBLE
    }

    companion object {
        private const val ARG_LINKED_TESTS = "linked_tests"
        private const val ARG_IS_ENROLLED = "is_enrolled"

        @JvmStatic
        fun newInstance(linkedTests: List<String>, isEnrolled: Boolean) =
            CourseTestsFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_LINKED_TESTS, ArrayList(linkedTests))
                    putBoolean(ARG_IS_ENROLLED, isEnrolled)
                }
            }
    }
}
