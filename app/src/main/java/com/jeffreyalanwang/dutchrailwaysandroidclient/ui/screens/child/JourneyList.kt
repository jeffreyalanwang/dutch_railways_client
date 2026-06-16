package com.jeffreyalanwang.dutchrailwaysandroidclient.ui.screens.child

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeffreyalanwang.dutchrailwaysandroidclient.Journey
import com.jeffreyalanwang.dutchrailwaysandroidclient.R
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.JourneyDetailNavArgs
import com.jeffreyalanwang.dutchrailwaysandroidclient.ui.util.AppStringFormats
import kotlinx.collections.immutable.ImmutableList

@Composable
fun JourneyList(
    journeys: ImmutableList<Journey>,
    onNavigate: (JourneyDetailNavArgs) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier) {
        if (journeys.isEmpty()) {
            item { NoJourneysPlaceholder() }
        } else journeys.forEachIndexed { i, journey ->
            item {
                JourneyListing(
                    journey,
                    { onNavigate(JourneyDetailNavArgs(i)) }
                )
            }
        }
    }
}

@Composable
private fun NoJourneysPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier
            .alpha(.7f)
            .fillMaxSize(),
        Arrangement.Center,
        Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_directions),
            null,
            Modifier
                .size(96.dp)
                .padding(bottom = 12.dp)
        )
        Text(
            "No routes available",
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun JourneyListing(
    journey: Journey,
    onClick: (Journey) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Row height is controlled by transfers column.
    // Element width is controlled by duration and transfers item/column.
    Row(
        modifier = modifier
            .clickable(onClick = { onClick(journey) })
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            AppStringFormats.TripDuration(journey.duration),
            textAlign = TextAlign.Center,
            softWrap = false,
            modifier = Modifier.fillMaxWidth(
                .25f
            ),
            autoSize = TextAutoSize.StepBased(
                MaterialTheme.typography.titleMedium.fontSize,
                MaterialTheme.typography.displayMedium.fontSize,
                stepSize = 1.sp,
            ),
            style = MaterialTheme.typography.displayMedium,
        )

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                AppStringFormats.Time(journey.stops.first().departure!!),
                style = MaterialTheme.typography.titleSmallEmphasized,
                modifier = Modifier.alpha(0.5f),
            )
            Text(
                AppStringFormats.Time(journey.stops.last().arrival!!),
                style = MaterialTheme.typography.titleSmallEmphasized,
                modifier = Modifier.alpha(0.5f),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                journey.stops.first()
                    .getStation().name,
                overflow = TextOverflow.MiddleEllipsis,
                style = MaterialTheme.typography.titleSmallEmphasized,
            )
            Text(
                journey.stops.last()
                    .getStation().name,
                overflow = TextOverflow.MiddleEllipsis,
                style = MaterialTheme.typography.titleSmallEmphasized,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                journey.transferCount.toString(),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                "Transfers",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}