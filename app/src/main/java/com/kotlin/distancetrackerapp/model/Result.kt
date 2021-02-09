package com.kotlin.distancetrackerapp.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 *@author Rizki Rian Anandita
 * Create By rizki
 */
@Parcelize
data class Result(

    var distance: String,

    var time: String

) : Parcelable