package com.antest1.kcanotify.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.antest1.kcanotify.KcaUtils
import com.antest1.kcanotify.LocaleUtils
import com.antest1.kcanotify.R
import com.antest1.kcanotify.ui.component.KcaScreen
import com.antest1.kcanotify.ui.theme.asTextSize
import com.antest1.kcanotify.viewmodel.FleetInfoViewModel
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

@Composable
fun FleetInfoScreen(
    viewModel: FleetInfoViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val scope = rememberCoroutineScope()
    val vibrator = remember(context) { KcaUtils.getVibrator(context) }

    var fleet by remember { mutableStateOf<FleetInfoViewModel.FleetData?>(null) }
    var loading by remember { mutableStateOf(true) }
    var contentVisible by remember { mutableStateOf(true) }
    var exportVisible by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf("") }

    fun loadFleet(portrait: Boolean) {
        loading = true
        scope.launch {
            val result = withContext(Dispatchers.IO) { viewModel.loadFleetData(portrait) }
            fleet = result
            contentVisible = result.passed
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        loadFleet(isPortrait)
    }

    KcaScreen(
        title = stringResource(R.string.action_fleetlist),
        onBackClick = onBack,
        actions = {
            TextButton(
                onClick = {
                    exportText = viewModel.deckBuilderData()
                    exportVisible = true
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            ) {
                Text(
                    text = stringResource(R.string.action_export_deckbuilder),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
            ) {
                InfoLine(
                    name = fleet?.fleetName ?: "",
                    fp = fleet?.fp ?: "",
                    seek = fleet?.seek ?: "",
                    onSelectFleet = {
                        showFleetDialog(context) { fleetIndex ->
                            viewModel.currentFleet = fleetIndex
                            loadFleet(isPortrait)
                        }
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    if (contentVisible) {
                        val rows = fleet?.rows
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (isPortrait) 1 else 2),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            if (rows != null) {
                                itemsIndexed(rows) { _, row ->
                                    ShipRow(
                                        row = row,
                                        combined = fleet?.isCombined == true,
                                    )
                                }
                            }
                        }
                        if (loading) {
                            LoadingOverlay()
                        }
                    }
                }
            }

            if (exportVisible) {
                ExportPanel(
                    text = exportText,
                    onDismiss = { exportVisible = false },
                    onCopy = {
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as ClipboardManager
                        clip.setPrimaryClip(ClipData.newPlainText("text", exportText))
                        KcaUtils.doVibrate(vibrator, 100)
                        Toast.makeText(
                            context.applicationContext,
                            context.getString(R.string.copied_to_clipboard),
                            Toast.LENGTH_LONG,
                        ).show()
                    },
                    onNoro6 = {
                        openUrl(context, "https://noro6.github.io/kc-web/#/aircalc")
                    },
                    onJervis = {
                        openUrl(context, "https://jervis.vercel.app/")
                    },
                    onImageBuilder = {
                        openImageBuilder(context, viewModel)
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Info line
// ---------------------------------------------------------------------------------------------

@Composable
private fun InfoLine(
    name: String,
    fp: String,
    seek: String,
    onSelectFleet: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = name,
            color = colorResource(R.color.black),
            fontSize = 18.dp.asTextSize(),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 10.dp),
        )
        Icon(
            painter = painterResource(R.drawable.ic_arrow_down),
            contentDescription = null,
            tint = colorResource(R.color.black),
            modifier = Modifier
                .size(24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelectFleet,
                ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = fp,
            color = colorResource(R.color.colorStatFighterPower),
            fontSize = 14.dp.asTextSize(),
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            text = seek,
            color = colorResource(R.color.colorStatSakuteki),
            fontSize = 14.dp.asTextSize(),
            modifier = Modifier.padding(end = 10.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Ship rows
// ---------------------------------------------------------------------------------------------

@Composable
private fun ShipRow(
    row: FleetInfoViewModel.FleetShipRow?,
    combined: Boolean,
) {
    val stripColor = colorResource(
        if (row?.cbFlag == true) R.color.colorStatSallyArea2 else R.color.colorStatSallyArea1
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (row == null) 0f else 1f)
                .padding(vertical = 4.dp)
                // `fship_cb_indicator`: 3dp strip spanning the row content, then 3dp gap.
                // Drawn behind the content so the row keeps its wrap-content height (the XML
                // `match_parent` strip does not stretch the row either).
                .drawBehind {
                    if (combined) {
                        drawRect(stripColor, size = Size(3.dp.toPx(), size.height))
                    }
                }
                .padding(start = if (combined) 6.dp else 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .padding(end = 4.dp)
                    .align(Alignment.CenterVertically),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = row?.stype ?: "",
                        color = colorResource(R.color.colorStatLocked),
                        fontSize = 12.dp.asTextSize(),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = 2.dp, end = 4.dp),
                    )
                    Text(
                        text = row?.name ?: "",
                        color = colorResource(R.color.colorStatNormal),
                        fontSize = 18.dp.asTextSize(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 2.dp, end = 2.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InfoText(row?.level ?: "", Modifier.weight(1f))
                    InfoText(row?.maxHp ?: "", Modifier.weight(1f))
                    InfoText(row?.speed ?: "", Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell(
                        iconRes = R.mipmap.stat_karyoku,
                        text = row?.karyoku ?: "",
                        color = colorResource(R.color.grey),
                        modifier = Modifier.weight(1f),
                    )
                    StatCell(
                        iconRes = R.mipmap.stat_raisou,
                        text = row?.raisou ?: "",
                        color = colorResource(R.color.grey),
                        modifier = Modifier.weight(1f),
                    )
                    StatCell(
                        iconRes = R.mipmap.stat_taisen,
                        text = row?.taisen ?: "",
                        color = if (row?.taisenHighlighted == true) {
                            colorResource(R.color.colorStatTaisen)
                        } else {
                            colorResource(R.color.grey)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    StatCell(
                        iconRes = R.mipmap.stat_taiku,
                        text = row?.taiku ?: "",
                        color = colorResource(R.color.grey),
                        modifier = Modifier.weight(1f),
                    )
                    StatCell(
                        iconRes = R.mipmap.stat_soukou,
                        text = row?.soukou ?: "",
                        color = colorResource(R.color.grey),
                        modifier = Modifier.weight(1f),
                    )
                    StatCell(
                        iconRes = R.mipmap.stat_lucky,
                        text = row?.luck ?: "",
                        color = if (row?.luckHighlighted == true) {
                            colorResource(R.color.colorStatLuck)
                        } else {
                            colorResource(R.color.grey)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
            ) {
                for (i in 0 until 5) {
                    val slot = row?.slots?.getOrNull(i)
                    // Rows 1-4 keep their space when empty; row 5 is GONE without an item.
                    if (i < 4 || slot != null) {
                        EquipRow(slot = slot, isFifth = i == 4)
                    }
                }
                ExArea(row?.exSlot)
            }
        }

        // `item_line`: always visible, filler rows included. Inset horizontally so the lines of
        // adjacent grid columns do not join into one continuous line in landscape.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .height(1.dp)
                .background(colorResource(R.color.colorAccent)),
        )
    }
}

@Composable
private fun InfoText(text: String, modifier: Modifier) {
    Text(
        text = text,
        color = colorResource(R.color.black),
        fontSize = 12.dp.asTextSize(),
        maxLines = 1,
        modifier = modifier.padding(start = 2.dp),
    )
}

@Composable
private fun StatCell(
    iconRes: Int,
    text: String,
    color: Color,
    modifier: Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            color = color,
            fontSize = 12.dp.asTextSize(),
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
        )
    }
}

@Composable
private fun EquipRow(
    slot: FleetInfoViewModel.EquipSlot?,
    isFifth: Boolean,
) {
    val cellBackground = if (isFifth) {
        colorResource(R.color.colorStatEquipSlotBack)
    } else {
        Color.Unspecified
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (slot == null) 0f else 1f),
        // XML rows are plain horizontal LinearLayouts: children are top aligned.
        verticalAlignment = Alignment.Top,
    ) {
        if (slot?.showSlotCount == true) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .then(
                        if (isFifth) Modifier.background(cellBackground) else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = slot.slotCountText,
                    color = colorResource(R.color.colorStatEquipSlot),
                    fontSize = 9.dp.asTextSize(),
                    maxLines = 1,
                )
            }
        } else if (!isFifth) {
            // Rows 1-4 keep the 10dp of the invisible slot-count cell; row 5 is GONE without it.
            Spacer(modifier = Modifier.size(10.dp))
        }
        if (slot != null) {
            Image(
                painter = painterResource(slot.iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(14.dp))
        }
        Text(
            text = slot?.name ?: "",
            color = colorResource(R.color.black),
            fontSize = 11.dp.asTextSize(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp),
        )
        Text(
            text = slot?.alvText ?: "",
            color = colorResource(slot?.alvColorRes ?: R.color.itemalv2),
            fontSize = 9.dp.asTextSize(),
            maxLines = 1,
            modifier = Modifier
                .then(
                    if (isFifth) Modifier.background(cellBackground) else Modifier
                )
                .padding(start = 2.dp),
        )
        Text(
            text = slot?.lvText ?: "",
            color = colorResource(R.color.itemlevel),
            fontSize = 9.dp.asTextSize(),
            maxLines = 1,
            modifier = Modifier
                .then(
                    if (isFifth) Modifier.background(cellBackground) else Modifier
                )
                .padding(start = 2.dp),
        )
    }
}

@Composable
private fun ExArea(exSlot: FleetInfoViewModel.EquipSlot?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (exSlot == null) 0f else 1f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Image(
                painter = painterResource(exSlot?.iconRes ?: R.mipmap.item_0),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(14.dp),
            )
            Text(
                text = exSlot?.name ?: "",
                color = colorResource(R.color.black),
                fontSize = 11.dp.asTextSize(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp),
            )
            Text(
                text = exSlot?.lvText ?: "",
                color = colorResource(R.color.itemlevel),
                fontSize = 9.dp.asTextSize(),
                maxLines = 1,
                modifier = Modifier.padding(start = 2.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Loading overlay
// ---------------------------------------------------------------------------------------------

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.5f)
            .background(colorResource(R.color.darkgray)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.fleetlist_loading),
            color = colorResource(R.color.white),
            fontSize = 40.dp.asTextSize(),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Export overlay
// ---------------------------------------------------------------------------------------------

@Composable
private fun ExportPanel(
    text: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onNoro6: () -> Unit,
    onJervis: () -> Unit,
    onImageBuilder: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .shadow(elevation = 3.dp)
            .background(colorResource(R.color.colorActivityBack))
            .padding(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            // `export_bar` is a RelativeLayout: the close icon sits at the top, not centred.
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(R.string.fleetinfo_export_title),
                color = colorResource(R.color.colorAccent),
                fontSize = 18.dp.asTextSize(),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 2.dp),
            )
            Icon(
                painter = painterResource(R.drawable.ic_cancel),
                contentDescription = null,
                tint = colorResource(R.color.colorAccent),
                modifier = Modifier.size(24.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colorResource(R.color.colorAccent)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                modifier = Modifier.padding(4.dp),
            )
        }
        ExportButton(stringResource(R.string.fleetinfo_export_clipboard), onCopy)
        ExportButton(stringResource(R.string.fleetinfo_export_openpage_noro6), onNoro6)
        ExportButton(stringResource(R.string.fleetinfo_export_openpage_jervisor), onJervis)
        ExportButton(stringResource(R.string.fleetinfo_export_openpage2), onImageBuilder)
    }
}

@Composable
private fun ExportButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .background(colorResource(R.color.colorAccent))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = colorResource(R.color.white),
            fontSize = 16.dp.asTextSize(),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Dialog and export actions
// ---------------------------------------------------------------------------------------------

private fun showFleetDialog(context: Context, onSelected: (Int) -> Unit) {
    val items = arrayOf(
        "1",
        "2",
        "3",
        "4",
        context.getString(R.string.fleetview_combined),
    )
    AlertDialog.Builder(context)
        .setSingleChoiceItems(items, -1) { dialog, which ->
            onSelected(which)
            dialog.dismiss()
        }
        .show()
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

private fun openImageBuilder(context: Context, viewModel: FleetInfoViewModel) {
    var data = viewModel.imageBuilderData()
    var localeCode = LocaleUtils.getResourceLocaleCode()
    if (localeCode == "ko") localeCode = "kr"
    if (localeCode == "tcn") localeCode = "jp"
    try {
        val addData = JsonParser.parseString(data).asJsonObject
        addData.addProperty("lang", localeCode)
        addData.addProperty("theme", "dark")
        data = addData.toString()
    } catch (_: Exception) {
        Toast.makeText(context.applicationContext, "json parsing error", Toast.LENGTH_SHORT).show()
    }

    try {
        val encoded = URLEncoder.encode(data, "utf-8")
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                ("https://kancolleimgbuilder.web.app/builder#$encoded").toUri(),
            )
        )
    } catch (_: UnsupportedEncodingException) {
        Toast.makeText(context.applicationContext, "parsing error", Toast.LENGTH_SHORT).show()
    }
}
