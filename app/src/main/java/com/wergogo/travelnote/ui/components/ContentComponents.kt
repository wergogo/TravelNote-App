package com.wergogo.travelnote.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wergogo.travelnote.data.model.ExpenseCategory
import com.wergogo.travelnote.ui.theme.CategoryFood
import com.wergogo.travelnote.ui.theme.CategoryOther
import com.wergogo.travelnote.ui.theme.CategoryShopping
import com.wergogo.travelnote.ui.theme.CategoryTransport
import com.wergogo.travelnote.ui.theme.Secondary
import com.wergogo.travelnote.util.DefaultImages
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Small avatar + nickname + relative timestamp shown on every expense/event/note.
 */
@Composable
fun AuthorTag(
    avatarId: String,
    nickname: String,
    createdAt: Long,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(DefaultImages.avatar(avatarId)),
            contentDescription = nickname,
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = nickname,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = relativeTime(createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Colored pill chip for an expense category. */
@Composable
fun CategoryChip(
    category: ExpenseCategory,
    modifier: Modifier = Modifier
) {
    val color = when (category) {
        ExpenseCategory.FOOD -> CategoryFood
        ExpenseCategory.TRANSPORT -> CategoryTransport
        ExpenseCategory.SHOPPING -> CategoryShopping
        ExpenseCategory.OTHER -> CategoryOther
    }
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = category.displayName(),
                style = MaterialTheme.typography.labelSmall
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.15f),
            labelColor = color
        ),
        modifier = modifier
    )
}

/** Coral-coloured currency amount with tabular figures. */
@Composable
fun AmountText(
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$currency %.2f".format(amount),
        style = MaterialTheme.typography.bodyMedium,
        color = Secondary,
        fontWeight = FontWeight.Medium,
        modifier = modifier
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun relativeTime(epochMillis: Long): String {
    if (epochMillis == 0L) return ""
    val now = System.currentTimeMillis()
    val diff = now - epochMillis
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        else -> {
            val fmt = DateTimeFormatter.ofPattern("MMM d")
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(fmt)
        }
    }
}
