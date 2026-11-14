package com.wergogo.travelnote.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wergogo.travelnote.data.model.Member
import com.wergogo.travelnote.data.model.Trip
import com.wergogo.travelnote.data.model.TripVisibility
import com.wergogo.travelnote.ui.theme.Success
import com.wergogo.travelnote.ui.theme.Warning
import com.wergogo.travelnote.util.DefaultImages
import com.wergogo.travelnote.util.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A card showing trip cover image, name, date range, member avatars, and status pill. */
@Composable
fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Cover image
            Image(
                painter = painterResource(DefaultImages.cover(trip.coverId)),
                contentDescription = trip.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TripStatusPill(trip)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDateRange(trip.startDate, trip.endDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (trip.memberIds.isNotEmpty()) {
                    Text(
                        text = "${trip.memberIds.size} member${if (trip.memberIds.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TripStatusPill(trip: Trip) {
    val today = LocalDate.now()
    val label = try {
        val start = LocalDate.parse(trip.startDate)
        val end = LocalDate.parse(trip.endDate)
        when {
            today.isBefore(start) -> "Upcoming"
            today.isAfter(end) -> "Past"
            else -> "Active"
        }
    } catch (e: Exception) { "—" }

    val color = when (label) {
        "Active" -> Success
        "Past" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color
        )
    )
}

/** Overlapping circular avatar row for trip member display. */
@Composable
fun MemberAvatarRow(
    members: List<Member>,
    maxVisible: Int = 4,
    modifier: Modifier = Modifier
) {
    val visible = members.take(maxVisible)
    val extra = members.size - visible.size

    Row(modifier = modifier) {
        visible.forEachIndexed { index, member ->
            Box(
                modifier = Modifier
                    .offset(x = (-8 * index).dp)
                    .size(32.dp)
                    .clip(CircleShape)
            ) {
                Image(
                    painter = painterResource(DefaultImages.avatar(member.avatarId)),
                    contentDescription = member.nickname,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (-8 * visible.size).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$extra",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Horizontal progress bar: spent vs total budget.
 * Green when under budget, coral/orange when >= 90%.
 */
@Composable
fun BudgetBar(
    spent: Double,
    total: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    val fraction = if (total > 0) (spent / total).coerceIn(0.0, 1.0).toFloat() else 0f
    val isWarning = spent / total.coerceAtLeast(0.01) >= 0.9
    val trackColor = if (isWarning) Warning else Success

    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(MaterialTheme.shapes.small),
            color = trackColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${formatMoney(currency, spent)} spent / ${"%.2f".format(total)} total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val remaining = total - spent
            Text(
                text = "${formatMoney(currency, remaining.coerceAtLeast(0.0))} left",
                style = MaterialTheme.typography.bodySmall,
                color = if (isWarning) Warning else Success,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDateRange(start: String, end: String): String = try {
    val fmt = DateTimeFormatter.ofPattern("MMM d")
    val s = LocalDate.parse(start).format(fmt)
    val e = LocalDate.parse(end).format(fmt)
    "$s \u2013 $e"
} catch (e: Exception) { "$start \u2013 $end" }
