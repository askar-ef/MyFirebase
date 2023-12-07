package com.example.myfirebase

import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.myfirebase.databinding.ActivityMainBinding
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var databaseReference: DatabaseReference
    private val budgetList = mutableListOf<Budget>()
    private var selectedBudgetId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        databaseReference = FirebaseDatabase.getInstance("https://myfirebase-86444-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference.child("budgets")

        with(binding) {
            btnAdd.setOnClickListener {
                val nominal = etNominal.text.toString()
                val description = etDesc.text.toString()
                val date = etDate.text.toString()
                Log.w("MainActivity", "Clicked on item: $description")
                val newBudget = Budget(nominal = nominal, description = description, date = date)
                addBudget(newBudget)
            }

            lvName.setOnItemClickListener { _, _, i, _ ->
                val item = budgetList[i]
                Log.d("MainActivity", "Clicked on item: $item")

                // Isi EditText dengan nilai yang dipilih dari ListView
                binding.etNominal.setText(item.nominal)
                binding.etDesc.setText(item.description)
                binding.etDate.setText(item.date)

                // Set selectedBudgetId
                selectedBudgetId = item.id
            }

            btnUpdate.setOnClickListener {
                val nominal = binding.etNominal.text.toString()
                val description = binding.etDesc.text.toString()
                val date = binding.etDate.text.toString()

                // Perbarui nilai di Firebase Realtime Database jika selectedBudgetId tidak null
                selectedBudgetId?.let { budgetId ->
                    val budgetReference = databaseReference.child(budgetId)
                    budgetReference.child("nominal").setValue(nominal)
                    budgetReference.child("description").setValue(description)
                    budgetReference.child("date").setValue(date)

                    // Kosongkan EditText setelah disimpan
                    binding.etNominal.setText("")
                    binding.etDesc.setText("")
                    binding.etDate.setText("")

                    // Reset selectedBudgetId
                    selectedBudgetId = null
                }
            }

            lvName.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, i, _ ->
                val item = budgetList[i]
                deleteBudget(item)
                true
            }
        }

        observeBudgetChanges()
        observeBudgets()
    }

    private fun observeBudgets() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            budgetList.map { "${it.nominal} - ${it.description} - ${it.date}" }
        )
        binding.lvName.adapter = adapter
    }

    private fun observeBudgetChanges() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val budgets = mutableListOf<Budget>()
                for (childSnapshot in snapshot.children) {
                    val budget = childSnapshot.getValue(Budget::class.java)
                    budget?.let { budgets.add(it) }
                }
                budgetList.clear()
                budgetList.addAll(budgets)
                observeBudgets()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MainActivity", "Error listening for budget changes: ", error.toException())
            }
        })
    }

    private fun addBudget(budget: Budget) {
        // Menggunakan push() untuk membuat ID baru
        val budgetId = databaseReference.push().key
        Log.w("MainActivity", "cek budgetId $budgetId")

        if (budgetId != null) {
            // Set nilai ID pada objek Budget
            budget.id = budgetId

            val budgetReference = databaseReference.child(budgetId)
            Log.w("MainActivity", "cek budgetReference $budgetReference")
            Log.w("MainActivity", "cek budget $budget")

            budgetReference.setValue(budget)
                .addOnSuccessListener {
                    Log.d("MainActivity", "Budget added successfully: $budget")
                }
                .addOnFailureListener {
                    Log.d("MainActivity", "Error adding budget: ", it)
                }
        } else {
            Log.d("MainActivity", "Error generating budget ID")
        }
    }

    private fun deleteBudget(budget: Budget) {
        if (budget.id.isNotEmpty()) {
            databaseReference.child(budget.id).removeValue()
                .addOnSuccessListener {
                    Log.d("MainActivity", "Budget deleted successfully: $budget")
                }
                .addOnFailureListener {
                    Log.d("MainActivity", "Error deleting budget: ", it)
                }
        } else {
            Log.d("MainActivity", "Error deleting budget: budget ID is empty!")
        }
    }
}
