package com.dicoding.bottomnavigationbar.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dicoding.bottomnavigationbar.R
import com.dicoding.bottomnavigationbar.data.LoginManager
import com.dicoding.bottomnavigationbar.data.retrofit.LoginResponse
import com.dicoding.bottomnavigationbar.databinding.ActivitySignInBinding
import com.dicoding.bottomnavigationbar.data.retrofit.Retro
import com.dicoding.bottomnavigationbar.data.retrofit.UserApi
import com.dicoding.bottomnavigationbar.ui.main.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignInActivity : AppCompatActivity() {

    private var binding : ActivitySignInBinding? = null
    private lateinit var auth : FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val loginManager by lazy { LoginManager(this) }
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        auth = Firebase.auth

        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding?.tvRegister?.setOnClickListener{
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
        binding?.btnSignIn?.setOnClickListener {
            lifecycleScope.launch {
                signInUser()
            }
        }

        binding?.btnSignInWithGoogle?.setOnClickListener{
            signInWithGoogle()
        }

        binding?.seePassword?.setOnCheckedChangeListener { _, isChecked ->
            binding?.etSinInPassword?.transformationMethod = if (isChecked) {
                HideReturnsTransformationMethod.getInstance()
            } else {
                PasswordTransformationMethod.getInstance()
            }
            binding?.etSinInPassword?.text?.let { binding?.etSinInPassword?.setSelection(it.length) }
        }
        progressBar = binding!!.progressBar

    }
    private fun signInUser() {

        val email = binding?.etSinInEmail?.text.toString()
        val password = binding?.etSinInPassword?.text.toString()

        if (validateForm(email, password)) {
            val retro = Retro().getRetroClientInstance().create(UserApi::class.java)

            retro.login(email = email, password = password).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    Log.i("SignInActivity", "onResponse: Start")
                    if (response.isSuccessful) {
                        val loginResponse = response.body()

                        if (loginResponse != null) {
                            val user = loginResponse.data

                            if (user != null) {

                                lifecycleScope.launch {
                                    loginManager.saveLoginData(user.email ?: "", loginResponse.data.accessToken ?: "",user.username?:"")
                                    startActivity(Intent(this@SignInActivity, MainActivity::class.java))
                                    Log.i("username", user.username!!)

                                    Log.i("email", user.email!!)
                                    showToast("Selamat Datang ${user.username}")
                                }
                            }
                        }
                    }
                }
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("Error", t.message!!)
                }
            })
        }
    }

    private fun signInWithGoogle()
    {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)

    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    { result ->
        if (result.resultCode == Activity.RESULT_OK)
        {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>){
        if (task.isSuccessful)
        {
            val account:GoogleSignInAccount? = task.result
            if (account!=null){
                updateUI(account)
            } else {
                showToast("Google Sign-In failed. Please try again.")
            }
        }
        else
        {
            showToast("Sign In Failed, Try Again Later")
        }
    }

    private fun updateUI(account: GoogleSignInAccount){
        progressBar.visibility = View.VISIBLE
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful)
            {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                progressBar.visibility = View.INVISIBLE
            }
            else
            {
                showToast("Can't login currently, Try after sometime")
                progressBar.visibility = View.INVISIBLE
            }
        }
    }

    private fun validateForm(email:String,password:String):Boolean
    {
        return when {
            TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()->{
                binding?.etSinInEmail?.error = "Enter valid email address"
                false
            }
            TextUtils.isEmpty(password)->{
                binding?.etSinInPassword?.error = "Enter password"
                false
            }
            else -> {
                binding?.etSinInEmail?.error = null
                true
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}