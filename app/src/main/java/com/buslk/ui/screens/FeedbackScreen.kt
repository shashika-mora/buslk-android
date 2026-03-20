package com.buslk.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buslk.data.FeedbackDoc
import com.buslk.ui.theme.BusLKBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    busId: String,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    var overallRating by remember { mutableStateOf(0) }
    var cleanlinessRating by remember { mutableStateOf(0) }
    var comfortRating by remember { mutableStateOf(0) }
    var driverRating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }

    // Mocking route
    val routeName = if (busId.contains("138")) "Route 138" else "Route 138"

    Scaffold(
        containerColor = Color(0xFFF5F6FA),
        bottomBar = {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val isSubmitEnabled = overallRating > 0
                    Button(
                        onClick = {
                            if (isSubmitEnabled) {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid
                                if (uid != null) {
                                    val db = FirebaseFirestore.getInstance()
                                    val feedbackRef = db.collection("feedbacks").document()
                                    val ratingsMap = mapOf(
                                        "overall" to overallRating,
                                        "cleanliness" to cleanlinessRating,
                                        "comfort" to comfortRating,
                                        "driver" to driverRating
                                    )
                                    
                                    val newFeedback = FeedbackDoc(
                                        id = feedbackRef.id,
                                        busId = busId,
                                        routeId = routeName,
                                        comment = comment,
                                        ratings = ratingsMap,
                                        tags = selectedTags.toList(),
                                        timestamp = com.google.firebase.Timestamp.now()
                                    )
                                    
                                    db.runTransaction { transaction ->
                                        transaction.set(feedbackRef, newFeedback)
                                        val userRef = db.collection("users").document(uid)
                                        val snapshot = transaction.get(userRef)
                                        val currentPoints = snapshot.getLong("points") ?: 0
                                        transaction.update(userRef, "points", currentPoints + 15)
                                    }.addOnSuccessListener {
                                        Toast.makeText(context, "Feedback submitted! +15 points", Toast.LENGTH_SHORT).show()
                                        onBackToHome()
                                    }.addOnFailureListener {
                                        Toast.makeText(context, "Failed to submit feedback", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    onBackToHome()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubmitEnabled) BusLKBlue.copy(alpha = 0.7f) else Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = isSubmitEnabled
                    ) {
                        Text("👍 Submit Feedback & Earn +15 Points", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Skip for now",
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBackToHome() }.padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Please provide at least an overall rating to submit",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // --- Blue Header ---
            Surface(
                color = BusLKBlue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(top = 48.dp, bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("✨", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("How was your trip?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$routeName • Private Bus", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // --- Overall Experience ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFDF5),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFEFA5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Overall Experience", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        RatingStars(currentRating = overallRating, onRatingChanged = { overallRating = it }, starSize = 48.dp)
                    }
                }

                // --- Rate Your Experience Details ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Rate Your Experience", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        DetailedRatingRow(title = "✨ Cleanliness", currentRating = cleanlinessRating, onRatingChanged = { cleanlinessRating = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailedRatingRow(title = "💨 Comfort", currentRating = comfortRating, onRatingChanged = { comfortRating = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailedRatingRow(title = "👥 Driver Behavior", currentRating = driverRating, onRatingChanged = { driverRating = it })
                    }
                }

                // --- Tell us more tags ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tell us more (optional)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        val chunkedTags = listOf(
                            listOf("✨ Clean Bus", "🛋️ Comfortable", "⏰ On Time"),
                            listOf("🛡️ Safe Driving", "😃 Friendly Staff", "👥 Too Crowded"),
                            listOf("⏱️ Delayed", "⚠️ Rough Driving")
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            chunkedTags.forEach { rowTags ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowTags.forEach { tag ->
                                        val isSelected = selectedTags.contains(tag)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BusLKBlue else Color.LightGray.copy(alpha = 0.5f)),
                                            color = if (isSelected) BusLKBlue.copy(alpha = 0.1f) else Color.White,
                                            modifier = Modifier.clickable {
                                                if (isSelected) selectedTags.remove(tag) else selectedTags.add(tag)
                                            }
                                        ) {
                                            Text(
                                                text = tag,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSelected) BusLKBlue else Color.DarkGray,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Additional Comments ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Additional Comments (optional)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = comment,
                            onValueChange = { if (it.length <= 500) comment = it },
                            placeholder = { Text("Share your experience to help us improve...", color = Color.LightGray) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedContainerColor = Color(0xFFF9F9F9),
                                focusedContainerColor = Color(0xFFF9F9F9)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${comment.length}/500 characters",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RatingStars(currentRating: Int, onRatingChanged: (Int) -> Unit, starSize: androidx.compose.ui.unit.Dp = 32.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= currentRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "Star $i",
                tint = if (i <= currentRating) Color(0xFFFFC107) else Color.LightGray,
                modifier = Modifier
                    .size(starSize)
                    .clickable { onRatingChanged(i) }
            )
        }
    }
}

@Composable
fun DetailedRatingRow(title: String, currentRating: Int, onRatingChanged: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            RatingStars(currentRating = currentRating, onRatingChanged = onRatingChanged, starSize = 24.dp)
        }
        
        Surface(
            shape = RoundedCornerShape(50),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            color = Color.White
        ) {
            Text(
                if (currentRating > 0) "$currentRating Rated" else "Not rated",
                fontSize = 10.sp,
                color = if (currentRating > 0) BusLKBlue else Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
