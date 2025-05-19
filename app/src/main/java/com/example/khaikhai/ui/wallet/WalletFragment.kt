package com.example.khaikhai.ui.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.khaikhai.R
import com.example.khaikhai.databinding.FragmentWalletBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private lateinit var walletViewModel: WalletViewModel
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        walletViewModel = ViewModelProvider(this).get(WalletViewModel::class.java)

        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Setup RecyclerView
        setupRecyclerView()

        // Observe ViewModel data
        observeViewModel()

        // Setup redeem button
        setupRedeemButton()

        return root
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        binding.rvTransactions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
        }
    }

    private fun observeViewModel() {
        // Observe cashback balance
        walletViewModel.cashbackBalance.observe(viewLifecycleOwner) { balance ->
            binding.textBalanceAmount.text = balance
        }

        // Observe transaction list
        walletViewModel.transactionList.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
        }

        // Observe loading state
        walletViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // You could show a progress indicator here if needed
        }

        // Observe errors
        walletViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRedeemButton() {
        binding.btnRedeem.setOnClickListener {
            // Get current balance
            val balanceText = binding.textBalanceAmount.text.toString()
            val balanceValue = balanceText.replace("$", "").toDoubleOrNull() ?: 0.0

            if (balanceValue <= 0) {
                Toast.makeText(requireContext(), "No cashback available to redeem", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show redemption dialog
            showRedemptionDialog(balanceValue)
        }
    }

    private fun showRedemptionDialog(maxAmount: Double) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_redeem_cashback, null)

        val amountEditText = dialogView.findViewById<TextInputEditText>(R.id.edit_text_amount)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Redeem Cashback")
            .setView(dialogView)
            .setPositiveButton("Redeem") { dialog, _ ->
                val amountStr = amountEditText.text.toString()
                val amount = amountStr.toDoubleOrNull()

                if (amount != null && amount > 0 && amount <= maxAmount) {
                    // Proceed with redemption
                    walletViewModel.redeemCashback(amount)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please enter a valid amount between $0.01 and $$maxAmount",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        walletViewModel.refreshWalletData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}