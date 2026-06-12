package com.example.myapplication.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.LearningCategory
import com.example.myapplication.model.LearningTutorial
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningResourcesPage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    
    var categories by remember { mutableStateOf<List<LearningCategory>>(emptyList()) }
    var tutorials by remember { mutableStateOf<List<LearningTutorial>>(emptyList()) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var expandedCategoryId by remember { mutableStateOf<String?>(null) }
    var debugMessage by remember { mutableStateOf("Loading...") }

    // Load categories
    LaunchedEffect(Unit) {
        android.util.Log.d("LearningResources", "=== LOADING CATEGORIES ===")
        debugMessage = "Connecting to Firestore..."
        
        // Use flat collection structure
        db.collection("learning_resources_categories")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("LearningResources", "❌ Error: ${error.message}")
                    debugMessage = "Error: ${error.message}"
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    android.util.Log.d("LearningResources", "📦 Documents found: ${snapshot.documents.size}")
                    debugMessage = "Found ${snapshot.documents.size} documents"
                    
                    if (snapshot.documents.isEmpty()) {
                        android.util.Log.w("LearningResources", "⚠️ No documents in learning_resources_categories")
                        debugMessage = "No documents in Firestore"
                    }
                    
                    snapshot.documents.forEach { doc ->
                        android.util.Log.d("LearningResources", "Doc ID: ${doc.id}")
                        android.util.Log.d("LearningResources", "Doc Data: ${doc.data}")
                    }
                    
                    categories = snapshot.documents.mapNotNull { doc ->
                        try {
                            val category = doc.toObject(LearningCategory::class.java)?.copy(categoryId = doc.id)
                            android.util.Log.d("LearningResources", "✅ Parsed: ${category?.categoryName}")
                            category
                        } catch (e: Exception) {
                            android.util.Log.e("LearningResources", "❌ Parse error: ${e.message}")
                            debugMessage = "Parse error: ${e.message}"
                            null
                        }
                    }.sortedBy { it.order }
                    
                    android.util.Log.d("LearningResources", "✅ Categories loaded: ${categories.size}")
                    debugMessage = "Loaded ${categories.size} categories"
                    isLoading = false
                } else {
                    android.util.Log.e("LearningResources", "❌ Snapshot is null")
                    debugMessage = "Snapshot is null"
                    isLoading = false
                }
            }
    }

    // Load tutorials
    LaunchedEffect(selectedCategoryId) {
        android.util.Log.d("LearningResources", "=== LOADING TUTORIALS ===")
        android.util.Log.d("LearningResources", "Selected Category ID: $selectedCategoryId")
        
        val query = if (selectedCategoryId != null) {
            db.collection("learning_resources_tutorials")
                .whereEqualTo("categoryId", selectedCategoryId)
        } else {
            db.collection("learning_resources_tutorials")
        }
        
        query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("LearningResources", "❌ Error loading tutorials: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    android.util.Log.d("LearningResources", "📦 Found ${snapshot.documents.size} tutorial documents")
                    
                    snapshot.documents.forEach { doc ->
                        android.util.Log.d("LearningResources", "Tutorial Doc ID: ${doc.id}")
                        android.util.Log.d("LearningResources", "Tutorial Data: ${doc.data}")
                    }
                    
                    tutorials = snapshot.documents.mapNotNull { doc ->
                        try {
                            val tutorial = doc.toObject(LearningTutorial::class.java)?.copy(tutorialId = doc.id)
                            android.util.Log.d("LearningResources", "✅ Parsed tutorial: ${tutorial?.title}")
                            tutorial
                        } catch (e: Exception) {
                            android.util.Log.e("LearningResources", "❌ Error parsing tutorial ${doc.id}: ${e.message}")
                            null
                        }
                    }.sortedBy { it.order } // Sort manually
                    
                    android.util.Log.d("LearningResources", "✅ Total tutorials loaded: ${tutorials.size}")
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back Button + Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                com.example.myapplication.globalNavigation.popBackStackSafely()
            }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Learning Resources",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp),
            placeholder = { 
                Text(
                    "Search tutorials...",
                    color = Color.Gray
                ) 
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Close, 
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (categories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No learning resources available yet",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = debugMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Categories and Tutorials List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Filter categories and tutorials based on search
                val filteredCategories = if (searchQuery.isNotEmpty()) {
                    categories.filter { category ->
                        category.categoryName.contains(searchQuery, ignoreCase = true) ||
                        tutorials.any { 
                            it.categoryId == category.categoryId && 
                            (it.title.contains(searchQuery, ignoreCase = true) ||
                             it.description.contains(searchQuery, ignoreCase = true))
                        }
                    }
                } else {
                    categories
                }

                items(filteredCategories) { category ->
                    CategoryCard(
                        category = category,
                        tutorials = tutorials.filter { 
                            it.categoryId == category.categoryId &&
                            (searchQuery.isEmpty() || 
                             it.title.contains(searchQuery, ignoreCase = true) ||
                             it.description.contains(searchQuery, ignoreCase = true))
                        },
                        isExpanded = expandedCategoryId == category.categoryId,
                        onExpandToggle = {
                            expandedCategoryId = if (expandedCategoryId == category.categoryId) {
                                null
                            } else {
                                category.categoryId
                            }
                        },
                        onTutorialClick = { tutorial ->
                            // Open tutorial URL
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tutorial.url))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: LearningCategory,
    tutorials: List<LearningTutorial>,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onTutorialClick: (LearningTutorial) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon/Emoji
                    Text(
                        text = category.icon.ifEmpty { "📚" },
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    
                    Column {
                        Text(
                            text = category.categoryName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${tutorials.size} tutorials",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Tutorials List (when expanded)
            if (isExpanded && tutorials.isNotEmpty()) {
                HorizontalDivider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tutorials.forEach { tutorial ->
                        TutorialItem(
                            tutorial = tutorial,
                            onClick = { onTutorialClick(tutorial) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialItem(
    tutorial: LearningTutorial,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type Icon
            Icon(
                imageVector = when (tutorial.type) {
                    "video" -> Icons.Default.PlayCircle
                    "article" -> Icons.Default.Article
                    "pdf" -> Icons.Default.PictureAsPdf
                    else -> Icons.Default.MenuBook
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tutorial.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tutorial.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
                if (tutorial.duration.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⏱️ ${tutorial.duration}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = Color.Gray
            )
        }
    }
}
