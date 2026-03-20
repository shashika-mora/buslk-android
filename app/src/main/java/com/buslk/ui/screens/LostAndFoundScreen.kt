@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostAndFoundScreen() {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("All (6)", "Found (4)", "Lost (2)")

    val displayedItems = when (selectedTabIndex) {
        1 -> mockLostFoundItems.filter { it.isFound }
        2 -> mockLostFoundItems.filter { !it.isFound }
        else -> mockLostFoundItems
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Surface(color = FriendsPurple, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Search, contentDescription = "Lost and Found Icon", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lost & Found", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Search items...", color = Color.White.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val routes = listOf("All Routes", "Route 138", "Route 176", "Route 120", "Route 177")
                    var selectedRoute by remember { mutableStateOf("All Routes") }

                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        routes.forEach { route ->
                            val isSelected = route == selectedRoute
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color.White else RoutePillActive,
                                modifier = Modifier.clickable { selectedRoute = route }
                            ) {
                                Text(
                                    text = route,
                                    color = if (isSelected) FriendsPurple else Color.White,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}