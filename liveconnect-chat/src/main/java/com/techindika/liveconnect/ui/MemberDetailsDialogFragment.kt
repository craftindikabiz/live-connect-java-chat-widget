package com.techindika.liveconnect.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.R
import com.techindika.liveconnect.model.VisitorProfile
import kotlinx.coroutines.*

/**
 * Bottom sheet dialog for collecting visitor details before starting chat.
 */
class MemberDetailsDialogFragment : BottomSheetDialogFragment() {

    var onProfileSubmitted: ((VisitorProfile) -> Unit)? = null
    var onCancelled: (() -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_member_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val theme = LiveConnectChat.currentTheme

        val nameInput = view.findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = view.findViewById<TextInputEditText>(R.id.emailInput)
        val phoneInput = view.findViewById<TextInputEditText>(R.id.phoneInput)
        val nameLayout = view.findViewById<TextInputLayout>(R.id.nameInputLayout)
        val emailLayout = view.findViewById<TextInputLayout>(R.id.emailInputLayout)
        val submitButton = view.findViewById<MaterialButton>(R.id.submitButton)

        submitButton.setBackgroundColor(theme.formButtonColor)
        submitButton.setTextColor(theme.formButtonTextColor)

        // Pre-fill if partial profile exists
        LiveConnectChat.visitorProfile?.let { profile ->
            nameInput.setText(profile.name)
            emailInput.setText(profile.email)
            phoneInput.setText(profile.phone)
        }

        submitButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()

            // Validate
            var valid = true
            if (name.length < 2) {
                nameLayout.error = "Name must be at least 2 characters"
                valid = false
            } else {
                nameLayout.error = null
            }
            // Accept either an email or a username — matches Flutter's
            // VisitorProfileValidator (no strict @ check, just length).
            if (email.isBlank() || email.length > 254) {
                emailLayout.error = "Email or username is required"
                valid = false
            } else {
                emailLayout.error = null
            }

            if (!valid) return@setOnClickListener

            submitButton.isEnabled = false
            val profile = VisitorProfile(name, email, phone)

            scope.launch {
                val success = LiveConnectChat.registerVisitorProfile(profile)
                if (!isAdded || view == null) return@launch
                if (success) {
                    onProfileSubmitted?.invoke(profile)
                    dismiss()
                } else {
                    submitButton.isEnabled = true
                    Toast.makeText(requireContext(), R.string.lc_error_network, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        onCancelled?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }

    companion object {
        fun newInstance() = MemberDetailsDialogFragment()
    }
}
