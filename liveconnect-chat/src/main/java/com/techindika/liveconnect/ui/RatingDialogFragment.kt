package com.techindika.liveconnect.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.techindika.liveconnect.LiveConnectChat
import com.techindika.liveconnect.R

/**
 * Star rating dialog shown after ticket resolution.
 */
class RatingDialogFragment : BottomSheetDialogFragment() {

    var onRatingSubmitted: ((Int) -> Unit)? = null
    private var selectedRating = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.dialog_rating, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val theme = LiveConnectChat.currentTheme
        val starContainer = view.findViewById<LinearLayout>(R.id.starContainer)
        val submitButton = view.findViewById<MaterialButton>(R.id.submitButton)
        val cancelButton = view.findViewById<MaterialButton>(R.id.cancelButton)

        submitButton.setBackgroundColor(theme.formButtonColor)

        // Create 5 star icons
        val stars = mutableListOf<ImageView>()
        for (i in 1..5) {
            val star = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(R.dimen.lc_rating_star_size),
                    resources.getDimensionPixelSize(R.dimen.lc_rating_star_size)
                ).apply {
                    marginEnd = 8
                }
                setImageResource(android.R.drawable.btn_star_big_off)
                setOnClickListener {
                    selectedRating = i
                    updateStars(stars, i)
                    submitButton.isEnabled = true
                }
            }
            stars.add(star)
            starContainer.addView(star)
        }

        submitButton.setOnClickListener {
            if (selectedRating > 0) {
                onRatingSubmitted?.invoke(selectedRating)
                dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun updateStars(stars: List<ImageView>, rating: Int) {
        for (i in stars.indices) {
            stars[i].setImageResource(
                if (i < rating) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
        }
    }

    companion object {
        fun newInstance() = RatingDialogFragment()
    }
}
