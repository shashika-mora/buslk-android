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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.buslk.ui.theme.BusLKBlue
import androidx.compose.ui.res.stringResource
import com.buslk.R
import com.buslk.ui.viewmodels.FeedbackViewModel
import com.buslk.ui.viewmodels.FeedbackUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    feedbackViewModel: FeedbackViewModel,
    busId: String,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current
    val uiState by feedbackViewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is FeedbackUiState.Success -> {
                Toast.makeText(context, context.getString(R.string.fb_toast_submitted), Toast.LENGTH_SHORT).show()
                feedbackViewModel.resetState()
                onBackToHome()
            }
            is FeedbackUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                feedbackViewModel.resetState()
            }
            else -> {}
        }
    }

    var overallRating by remember { mutableStateOf(0) }
    var cleanlinessRating by remember { mutableStateOf(0) }
    var comfortRating by remember { mutableStateOf(0) }
    var driverRating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }

    val loadingStr = context.getString(R.string.fb_loading)
    val unknownRouteStr = context.getString(R.string.fb_unknown_route)
    var routeName by remember { mutableStateOf(loadingStr) }
    LaunchedEffect(busId) {
        val repo = com.buslk.data.SearchRepository()
        val busData = repo.getBusDetails(busId).getOrNull()
        routeName = busData?.defaultRouteId ?: unknownRouteStr
    }

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
                            if (isSubmitEnabled && uiState != FeedbackUiState.Submitting) {
                                feedbackViewModel.submitFeedback(
                                    busId = busId,
                                    overallRating = overallRating,
                                    cleanlinessRating = cleanlinessRating,
                                    comfortRating = comfortRating,
                                    driverRating = driverRating,
                                    comment = comment,
                                    tags = selectedTags.toList()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubmitEnabled) BusLKBlue.copy(alpha = 0.7f) else Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = isSubmitEnabled
                    ) {
                        Text(stringResource(id = R.string.fb_btn_submit), color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(id = R.string.fb_btn_skip),
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBackToHome() }.padding(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        stringResource(id = R.string.fb_submit_prompt),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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
                    Text(stringResource(id = R.string.fb_header_title), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(id = R.string.fb_header_subtitle_fmt, routeName), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
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
                        Text(stringResource(id = R.string.fb_section_overall), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        RatingStars(currentRating = overallRating, onRatingChanged = { overallRating = it }, starSize = 48.dp)
                        
                        val ratingText = when (overallRating) {
                            1 -> stringResource(id = R.string.fb_rating_1)
                            2 -> stringResource(id = R.string.fb_rating_2)
                            3 -> stringResource(id = R.string.fb_rating_3)
                            4 -> stringResource(id = R.string.fb_rating_4)
                            5 -> stringResource(id = R.string.fb_rating_5)
                            else -> null
                        }
                        
                        if (ratingText != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(ratingText, color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                // --- Rate Your Experience Details ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(id = R.string.fb_section_rate_exp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        DetailedRatingRow(title = stringResource(id = R.string.fb_row_cleanliness), currentRating = cleanlinessRating, onRatingChanged = { cleanlinessRating = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailedRatingRow(title = stringResource(id = R.string.fb_row_comfort), currentRating = comfortRating, onRatingChanged = { comfortRating = it })
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailedRatingRow(title = stringResource(id = R.string.fb_row_driver), currentRating = driverRating, onRatingChanged = { driverRating = it })
                    }
                }

                // --- Tell us more tags ---
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(id = R.string.fb_section_tell_more), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        data class TagInfo(val dbKey: String, val resId: Int)
                        val chunkedTags = listOf(
                            listOf(
                                TagInfo("clean_bus", R.string.fb_tag_clean_bus),
                                TagInfo("comfortable", R.string.fb_tag_comfortable),
                                TagInfo("on_time", R.string.fb_tag_on_time)
                            ),
                            listOf(
                                TagInfo("safe_driving", R.string.fb_tag_safe_driving),
                                TagInfo("friendly_staff", R.string.fb_tag_friendly_staff),
                                TagInfo("too_crowded", R.string.fb_tag_too_crowded)
                            ),
                            listOf(
                                TagInfo("delayed", R.string.fb_tag_delayed),
                                TagInfo("rough_driving", R.string.fb_tag_rough_driving)
                            )
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            chunkedTags.forEach { rowTags ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    rowTags.forEach { tagInfo ->
                                        val isSelected = selectedTags.contains(tagInfo.dbKey)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BusLKBlue else Color.LightGray.copy(alpha = 0.5f)),
                                            color = if (isSelected) BusLKBlue.copy(alpha = 0.1f) else Color.White,
                                            modifier = Modifier.clickable {
                                                if (isSelected) selectedTags.remove(tagInfo.dbKey) else selectedTags.add(tagInfo.dbKey)
                                            }
                                        ) {
                                            Text(
                                                text = stringResource(id = tagInfo.resId),
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
                        Text(stringResource(id = R.string.fb_section_comments), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = comment,
                            onValueChange = { if (it.length <= 500) comment = it },
                            placeholder = { Text(stringResource(id = R.string.fb_comments_placeholder), color = Color.LightGray) },
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
                            stringResource(id = R.string.fb_char_count_fmt, comment.length),
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
}

@Composable
fun RatingStars(currentRating: Int, onRatingChanged: (Int) -> Unit, starSize: androidx.compose.ui.unit.Dp = 32.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= currentRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(id = R.string.fb_star_desc, i),
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
                if (currentRating > 0) stringResource(id = R.string.fb_rated_fmt, currentRating) else stringResource(id = R.string.fb_not_rated),
                fontSize = 10.sp,
                color = if (currentRating > 0) BusLKBlue else Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
