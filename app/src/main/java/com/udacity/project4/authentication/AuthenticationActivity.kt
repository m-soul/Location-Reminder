package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import kotlinx.android.synthetic.main.activity_authentication.*
import kotlinx.android.synthetic.main.activity_reminders.*
import org.koin.android.ext.android.get

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {
    companion object{const val signInRequestCode = 0}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null)
        {
            val intent = Intent(this, RemindersActivity::class.java)
            startActivity(intent)
            finish()
        }

        button.setOnClickListener { launchSignIn() }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthenticationActivity.signInRequestCode)
        {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK)
            {
                val intent = Intent(this, RemindersActivity::class.java)
                startActivity(intent)
                finish()
            } else
            {
                Log.i("login","Sign in error ${response?.error?.errorCode}")
            }
        }
    }
    private fun launchSignIn()
    {
        val signInOptions = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(), AuthUI.IdpConfig.GoogleBuilder().build())

        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(signInOptions).build(),AuthenticationActivity.signInRequestCode)
    }
}
