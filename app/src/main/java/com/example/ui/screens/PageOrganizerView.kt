package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.PageAnnotationEntity
import com.example.data.local.PageTemplateType
import com.example.data.local.PdfDocumentEntity

@Composable
fun PageOrganizerView(
    document: PdfDocumentEntity,
    currentPageIndex: Int,
    pageThumbnails: Map<Int, Bitmap>,
    annotations: List<PageAnnotationEntity>,
    onSelectPage: (Int) -> Unit,
    onRotatePage: (Int) -> Unit,
    onMovePage: (fromIndex: Int, toIndex: Int) -> Unit,
    onDuplicatePage: (Int) -> Unit,
    onExtractPage: (Int) -> Unit,
    onDeletePage: (Int) -> Unit,
    onAppendTemplatePage: (PageTemplateType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("page_organizer_view")
    ) {
        // Header banner explaining page operations
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Page Organizer & Structure",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Rotate 90°, reorder, duplicate, extract, or insert new template pages.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${document.pageCount} PAGES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Quick Append Page bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Insert Page:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PageTemplateType.entries.forEach { template ->
                        FilledTonalButton(
                            onClick = { onAppendTemplatePage(template) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("append_page_${template.name.lowercase()}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(template.title.substringBefore(" "), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 165.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(document.pageCount, key = { it }) { pageIdx ->
                val isCurrent = pageIdx == currentPageIndex
                val thumb = pageThumbnails[pageIdx]
                val pageAnnCount = annotations.count { it.pageIndex == pageIdx }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 5.dp else 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isCurrent) 2.dp else 1.dp,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectPage(pageIdx) }
                        .testTag("organizer_page_card_$pageIdx")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.71f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFDFBF7))
                                .border(1.dp, Color(0xFFE0D8C8), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb.asImageBitmap(),
                                    contentDescription = "Page ${pageIdx + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Page number badge
                            Surface(
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else Color(0xCC191A1E),
                                shape = CircleShape,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "P.${pageIdx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (pageAnnCount > 0) {
                                Surface(
                                    color = Color(0xFFE59800),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "$pageAnnCount ink",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF191A1E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Action toolbar row 1: Move Earlier, Rotate 90, Move Later
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onMovePage(pageIdx, pageIdx - 1) },
                                enabled = pageIdx > 0,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("move_page_prev_$pageIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Move page earlier",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { onRotatePage(pageIdx) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("rotate_page_$pageIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RotateRight,
                                    contentDescription = "Rotate page 90 degrees",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            IconButton(
                                onClick = { onMovePage(pageIdx, pageIdx + 1) },
                                enabled = pageIdx < document.pageCount - 1,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("move_page_next_$pageIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Move page later",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Action toolbar row 2: Duplicate, Extract as PDF, Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onDuplicatePage(pageIdx) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("duplicate_page_$pageIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate page",
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = { onExtractPage(pageIdx) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("extract_page_$pageIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.IosShare,
                                    contentDescription = "Extract page as standalone PDF",
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeletePage(pageIdx) },
                                enabled = document.pageCount > 1,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("delete_page_$pageIdx")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete page",
                                    tint = if (document.pageCount > 1) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
