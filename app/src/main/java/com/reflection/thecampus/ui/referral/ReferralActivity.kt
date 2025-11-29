package com.reflection.thecampus.ui.referral

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.reflection.thecampus.R
import com.reflection.thecampus.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReferralActivity : AppCompatActivity() {

    private val viewModel: ReferralViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_referral)

        // Set status bar color to match surface (toolbar)
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        window.statusBarColor = typedValue.data

        // Set status bar icon appearance based on theme
        val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkMode

        // Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Views
        val tvReferralCode = findViewById<TextView>(R.id.tvReferralCode)
        val btnCopyCode = findViewById<View>(R.id.btnCopyCode)
        val btnShareCode = findViewById<View>(R.id.btnShareCode)
        val tvWalletBalance = findViewById<TextView>(R.id.tvWalletBalance)
        val tvTotalEarned = findViewById<TextView>(R.id.tvTotalEarned)
        val btnWithdraw = findViewById<View>(R.id.btnWithdraw)
        val rvTransactions = findViewById<RecyclerView>(R.id.rvTransactions)
        val tvNoTransactions = findViewById<TextView>(R.id.tvNoTransactions)

        // Setup RecyclerView
        adapter = TransactionAdapter()
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        // Observers
        viewModel.referralCode.observe(this) { code ->
            tvReferralCode.text = code
        }

        viewModel.wallet.observe(this) { wallet ->
            tvWalletBalance.text = "₹${wallet.balance}"
            tvTotalEarned.text = "₹${wallet.totalEarned}"

            val transactions = wallet.transactions.values.sortedByDescending { it.timestamp }
            if (transactions.isEmpty()) {
                tvNoTransactions.visibility = View.VISIBLE
                rvTransactions.visibility = View.GONE
            } else {
                tvNoTransactions.visibility = View.GONE
                rvTransactions.visibility = View.VISIBLE
                adapter.submitList(transactions)
            }
        }

        viewModel.withdrawalStatus.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Actions
        btnCopyCode.setOnClickListener {
            val code = tvReferralCode.text.toString()
            if (code != "LOADING...") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Referral Code", code)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Code copied!", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Share button click listener
        btnShareCode.setOnClickListener {
            val code = tvReferralCode.text.toString()
            if (code != "LOADING...") {
                shareReferralLink(code)
            }
        }

        btnWithdraw.setOnClickListener {
            showWithdrawDialog()
        }
    }

    private fun shareReferralLink(code: String) {
        val link = "https://app.thecampus.in/signup?ref=$code"
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Join The Campus!")
            putExtra(android.content.Intent.EXTRA_TEXT, "Hey! Join The Campus using my referral code $code and get rewards: $link")
        }
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Referral Link"))
    }

    private fun showWithdrawDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_withdraw, null)
        val etUpiId = dialogView.findViewById<EditText>(R.id.etUpiId)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val btnCancel = dialogView.findViewById<View>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<View>(R.id.btnConfirmWithdraw)

        val currentBalance = viewModel.wallet.value?.balance ?: 0.0
        etAmount.setText(currentBalance.toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Make background transparent for CardView corner radius
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val upiId = etUpiId.text.toString()
            val amountStr = etAmount.text.toString()
            val amount = amountStr.toDoubleOrNull() ?: 0.0

            if (upiId.isEmpty()) {
                etUpiId.error = "Enter UPI ID"
                return@setOnClickListener
            }

            if (amount < 100) {
                Toast.makeText(this, "Minimum withdrawal is ₹100", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (amount > currentBalance) {
                Toast.makeText(this, "Insufficient balance", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.requestWithdrawal(amount, upiId)
            dialog.dismiss()
        }

        dialog.show()
    }
}

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    private var transactions: List<Transaction> = emptyList()

    fun submitList(list: List<Transaction>) {
        transactions = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount() = transactions.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon = itemView.findViewById<ImageView>(R.id.ivTransactionIcon)
        private val tvDescription = itemView.findViewById<TextView>(R.id.tvDescription)
        private val tvDate = itemView.findViewById<TextView>(R.id.tvDate)
        private val tvAmount = itemView.findViewById<TextView>(R.id.tvAmount)
        private val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)

        fun bind(transaction: Transaction) {
            tvDescription.text = transaction.description ?: "Transaction"
            
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            tvDate.text = dateFormat.format(Date(transaction.timestamp))

            if (transaction.type == "credit") {
                tvAmount.text = "+ ₹${transaction.amount}"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                ivIcon.setImageResource(R.drawable.ic_arrow_downward) // Incoming
            } else {
                tvAmount.text = "- ₹${transaction.amount}"
                tvAmount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_red_dark))
                ivIcon.setImageResource(R.drawable.ic_arrow_upward) // Outgoing
            }

            tvStatus.text = transaction.status?.replace("_", " ")?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: ""
        }
    }
}
