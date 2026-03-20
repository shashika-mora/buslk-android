// Inside LostAndFoundScreen Column, after route filters

// Post New Item Button
Surface(modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(8.dp), color = Color.Transparent) {
    Button(
        onClick = { /* TODO */ },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OpenGreen),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Post New Item", fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

// Tabs
TabRow(
selectedTabIndex = selectedTabIndex,
containerColor = Color(0xFFF5F6FA),
contentColor = Color.Black,
divider = {},
indicator = { tabPositions -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]), color = Color.Transparent) },
modifier = Modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(24.dp))
) {
    tabs.forEachIndexed { index, title ->
        val selected = selectedTabIndex == index
        Tab(
            selected = selected,
            onClick = { selectedTabIndex = index },
            modifier = Modifier.background(if (selected) Color.White else Color.Transparent).clip(RoundedCornerShape(50))
        ) {
            Text(text = title, modifier = Modifier.padding(vertical = 12.dp), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
        }
    }
}