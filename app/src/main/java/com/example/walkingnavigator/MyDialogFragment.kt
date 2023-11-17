package com.example.walkingnavigator

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class MyDialogFragment(var countSteps:Int): DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Вы прошли $countSteps шагов!")
                .setMessage("Хотите закончить прогулку?")
                .setPositiveButton("Да") {
                        dialog, _ ->  dialog.cancel()
                    val activity =  activity as MainActivity
                    activity.clearMap()
                }
                .setNegativeButton("Нет"){
                        dialog, _ ->  dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("IllegalStateException")
    }
}
