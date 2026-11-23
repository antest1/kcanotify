package com.antest1.kcanotify.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antest1.kcanotify.KcaApiData
import com.antest1.kcanotify.KcaExpedition2
import com.antest1.kcanotify.KcaUtils
import com.antest1.kcanotify.LocaleUtils
import com.antest1.kcanotify.R
import com.antest1.kcanotify.ui.component.KcaChip
import com.antest1.kcanotify.ui.component.KcaScreen
import com.antest1.kcanotify.ui.theme.KcaColors
import com.antest1.kcanotify.ui.theme.asTextSize
import com.antest1.kcanotify.viewmodel.ExpeditionTableViewModel
import com.google.gson.JsonObject

/** World filter chips, in the order the XML `ChipGroup` declares them. */
private val WorldChips = listOf(
    R.string.expdtable_btn_w0 to 0,
    R.string.expdtable_btn_w1 to 1,
    R.string.expdtable_btn_w2 to 2,
    R.string.expdtable_btn_w3 to 3,
    R.string.expdtable_btn_w7 to 7,
    R.string.expdtable_btn_w4 to 4,
    R.string.expdtable_btn_w5 to 5,
    R.string.expdtable_btn_w6 to 6,
)

/**
 * Expedition table screen: world filter chips, the great-success/daihatsu modifiers and the
 * expandable expedition list. See `docs/screens/expedition-table.md` for the full spec.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpeditionTableScreen(
    viewModel: ExpeditionTableViewModel,
    onBack: () -> Unit,
) {
    var worldIdx by remember { mutableIntStateOf(viewModel.worldIdx) }
    var greatSuccess by remember { mutableStateOf(viewModel.isGreatSuccess) }
    var daihatsuCount by remember { mutableIntStateOf(viewModel.daihatsuCount) }
    // Matches the adapter: expansion survives world changes, is lost when the activity is recreated.
    var expanded by remember { mutableStateOf(emptySet<Int>()) }

    val items = remember(worldIdx) { viewModel.expeditionItems }

    KcaScreen(
        title = stringResource(R.string.action_expdtable),
        onBackClick = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
        ) {
            FilterChipRow(
                worldIdx = worldIdx,
                greatSuccess = greatSuccess,
                daihatsuCount = daihatsuCount,
                greatSuccessText = viewModel.greatSuccessText,
                onWorldClick = { world ->
                    viewModel.worldIdx = world
                    worldIdx = world
                },
                onGreatSuccessClick = {
                    viewModel.toggleGreatSuccess()
                    greatSuccess = viewModel.isGreatSuccess
                },
                onDaihatsuClick = {
                    viewModel.cycleDaihatsuCount()
                    daihatsuCount = viewModel.daihatsuCount
                },
            )
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 4.dp),
            ) {
                items(items, key = { it.get("no").asInt }) { item ->
                    val no = item.get("no").asInt
                    ExpeditionRow(
                        item = item,
                        no = no,
                        viewModel = viewModel,
                        greatSuccess = greatSuccess,
                        daihatsuCount = daihatsuCount,
                        expanded = expanded.contains(no),
                        onToggle = {
                            expanded = if (expanded.contains(no)) {
                                expanded - no
                            } else {
                                expanded + no
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipRow(
    worldIdx: Int,
    greatSuccess: Boolean,
    daihatsuCount: Int,
    greatSuccessText: String,
    onWorldClick: (Int) -> Unit,
    onGreatSuccessClick: () -> Unit,
    onDaihatsuClick: () -> Unit,
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 5.dp),
        // `app:chipSpacingVertical="-8dp"`: wrapped rows overlap slightly.
        verticalArrangement = Arrangement.spacedBy((-8).dp),
    ) {
        WorldChips.forEach { (textRes, world) ->
            val container = if (world == 0) {
                colorResource(R.color.colorBtn)
            } else {
                colorResource(worldColorRes(world))
            }
            val content = if (world == 0) {
                colorResource(R.color.colorBtnText)
            } else {
                KcaColors.White
            }
            KcaChip(
                text = stringResource(textRes),
                onClick = { onWorldClick(world) },
                modifier = Modifier.padding(3.dp),
                containerColor = container,
                contentColor = content,
                textSize = 15.dp.asTextSize(),
            )
        }

        // `Widget.Material3.Chip.Filter`, never selected: only the label switches Normal / Great.
        FilterChip(
            selected = false,
            onClick = onGreatSuccessClick,
            label = {
                Text(
                    text = greatSuccessText,
                    fontSize = 15.dp.asTextSize(),
                    color = colorResource(R.color.colorBtnText),
                )
            },
            modifier = Modifier.padding(3.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            colors = FilterChipDefaults.filterChipColors(
                containerColor = KcaColors.ChipSurface,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = KcaColors.ChipOutline,
            ),
        )

        // Plain `<Chip>`: Material 3 assist chip with the drum icon.
        AssistChip(
            onClick = onDaihatsuClick,
            label = {
                Text(
                    text = ExpeditionTableViewModel.getDaihatsuCountString(daihatsuCount),
                    fontSize = 15.dp.asTextSize(),
                    color = colorResource(R.color.colorBtnText),
                )
            },
            leadingIcon = {
                Image(
                    painter = painterResource(R.mipmap.item_20),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
            modifier = Modifier.padding(3.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = KcaColors.ChipSurface,
            ),
            border = BorderStroke(1.dp, KcaColors.ChipOutline),
        )
    }
}

private fun worldColorRes(world: Int): Int = when (world) {
    1 -> R.color.colorExpeditionTable1
    2 -> R.color.colorExpeditionTable2
    3 -> R.color.colorExpeditionTable3
    4 -> R.color.colorExpeditionTable4
    5 -> R.color.colorExpeditionTable5
    6 -> R.color.colorExpeditionTable6
    else -> R.color.colorExpeditionTable7
}

// ---------------------------------------------------------------------------------------------
// Rows
// ---------------------------------------------------------------------------------------------

@Composable
private fun ExpeditionRow(
    item: JsonObject,
    no: Int,
    viewModel: ExpeditionTableViewModel,
    greatSuccess: Boolean,
    daihatsuCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val areaColor = colorResource(viewModel.getAreaColorRes(item.get("area").asInt))
    val valueColor = if (greatSuccess) {
        colorResource(R.color.colorExpeditionGreatSuccess)
    } else {
        colorResource(R.color.black)
    }
    if (expanded) {
        ExpeditionRowFull(
            item, no, areaColor, valueColor, greatSuccess, daihatsuCount, viewModel, onToggle
        )
    } else {
        ExpeditionRowAbstract(
            item, no, areaColor, valueColor, greatSuccess, daihatsuCount, viewModel, onToggle
        )
    }
}

@Composable
private fun ExpeditionRowAbstract(
    item: JsonObject,
    no: Int,
    areaColor: Color,
    valueColor: Color,
    greatSuccess: Boolean,
    daihatsuCount: Int,
    viewModel: ExpeditionTableViewModel,
    onToggle: () -> Unit,
) {
    val resourceValues = remember(item, greatSuccess, daihatsuCount) {
        val resource = item.getAsJsonArray("resource")
        IntArray(resource.size()) { viewModel.calcResourcesBonus(resource.get(it).asInt) }
    }
    val reward = item.getAsJsonArray("reward")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggle,
                ),
        ) {
            Column(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                TableText(
                    text = KcaExpedition2.getExpeditionStr(no),
                    modifier = Modifier.fillMaxWidth().height(20.dp),
                    color = KcaColors.White,
                    fontSize = 12.dp,
                    bold = true,
                    background = areaColor,
                )
                TableText(
                    text = expeditionName(item),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    color = areaColor,
                    fontSize = 12.dp,
                    bold = true,
                )
            }
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(areaColor),
                ) {
                    TableText(
                        // `android:textAllCaps="true"` on the collapsed header labels.
                        text = stringResource(R.string.expdtable_label_time)
                            .uppercase(LocaleUtils.getLocale()),
                        modifier = Modifier.weight(0.5f).fillMaxHeight(),
                        color = KcaColors.White,
                        fontSize = 12.dp,
                    )
                    Row(modifier = Modifier.weight(1f)) {
                        TableText(
                            text = item.get("total-num").asString,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            color = KcaColors.White,
                            fontSize = 12.dp,
                        )
                        TableText(
                            text = if (item.has("flag-lv")) {
                                ("Lv " + item.get("flag-lv").asString)
                                    .uppercase(LocaleUtils.getLocale())
                            } else {
                                ""
                            },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            color = KcaColors.White,
                            fontSize = 12.dp,
                        )
                    }
                    TableText(
                        text = stringResource(R.string.expdtable_label_item)
                            .uppercase(LocaleUtils.getLocale()),
                        modifier = Modifier.weight(0.5f).fillMaxHeight(),
                        color = KcaColors.White,
                        fontSize = 10.dp,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                    TableText(
                        text = KcaUtils.getTimeStr(item.get("time").asInt, true),
                        modifier = Modifier.weight(0.5f).fillMaxHeight(),
                        color = valueColor,
                        fontSize = 12.dp,
                    )
                    TableText(
                        text = resourceValues.joinToString("/"),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        color = valueColor,
                        fontSize = 11.dp,
                    )
                    Row(modifier = Modifier.weight(0.5f).fillMaxHeight()) {
                        RewardIcon(
                            type = reward.get(0).asJsonArray.get(0).asInt,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        RewardIcon(
                            type = reward.get(1).asJsonArray.get(0).asInt,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpeditionRowFull(
    item: JsonObject,
    no: Int,
    areaColor: Color,
    valueColor: Color,
    greatSuccess: Boolean,
    daihatsuCount: Int,
    viewModel: ExpeditionTableViewModel,
    onToggle: () -> Unit,
) {
    val exp = item.getAsJsonArray("exp")
    val multiplier = if (greatSuccess) 2 else 1
    val resourceValues = remember(item, greatSuccess, daihatsuCount) {
        val resource = item.getAsJsonArray("resource")
        IntArray(resource.size()) { viewModel.calcResourcesBonus(resource.get(it).asInt) }
    }
    val reward = item.getAsJsonArray("reward")
    val reward1 = reward.get(0).asJsonArray
    val reward2 = reward.get(1).asJsonArray

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.wrapContentWidth().widthIn(min = 32.dp)) {
                TableText(
                    text = stringResource(R.string.expdtable_label_no),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(areaColor)
                        .padding(vertical = 2.dp),
                    color = KcaColors.White,
                    fontSize = 14.dp,
                    bold = true,
                )
                TableText(
                    text = KcaExpedition2.getExpeditionStr(no),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    color = areaColor,
                    fontSize = 14.dp,
                    bold = true,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                // Name / time / HQ exp / ship exp header
                Row(modifier = Modifier.fillMaxWidth().background(areaColor)) {
                    TableText(
                        text = stringResource(R.string.expdtable_label_name),
                        modifier = Modifier.weight(3f).padding(vertical = 2.dp),
                        color = KcaColors.White,
                        fontSize = 14.dp,
                        bold = true,
                    )
                    TableText(
                        text = stringResource(R.string.expdtable_label_time),
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                        color = KcaColors.White,
                        fontSize = 14.dp,
                        bold = true,
                    )
                    TableText(
                        text = stringResource(R.string.expdtable_label_hq_exp),
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                        color = KcaColors.White,
                        fontSize = 14.dp,
                        bold = true,
                    )
                    TableText(
                        text = stringResource(R.string.expdtable_label_ship_exp),
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                        color = KcaColors.White,
                        fontSize = 14.dp,
                        bold = true,
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    TableText(
                        text = expeditionName(item),
                        modifier = Modifier.weight(3f).padding(vertical = 2.dp),
                        color = areaColor,
                        fontSize = 14.dp,
                        bold = true,
                        // The XML `value_name` has no `maxLines`, so long names wrap.
                        singleLine = false,
                    )
                    TableText(
                        text = KcaUtils.getTimeStr(item.get("time").asInt, true),
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                        color = valueColor,
                        fontSize = 14.dp,
                        singleLine = false,
                    )
                    TableText(
                        text = (exp.get(0).asInt * multiplier).toString(),
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                        color = valueColor,
                        fontSize = 14.dp,
                        singleLine = false,
                    )
                    TableText(
                        text = (exp.get(1).asInt * multiplier).toString(),
                        modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                        color = valueColor,
                        fontSize = 14.dp,
                        singleLine = false,
                    )
                }

                // Resources / rewards header
                Row(modifier = Modifier.fillMaxWidth().background(areaColor)) {
                    RewardHeaderIcon(R.mipmap.icon_fuel)
                    RewardHeaderIcon(R.mipmap.icon_ammo)
                    RewardHeaderIcon(R.mipmap.icon_steel)
                    RewardHeaderIcon(R.mipmap.icon_bauxite)
                    TableText(
                        text = stringResource(R.string.expdtable_label_item1),
                        modifier = Modifier.weight(1f).height(20.dp),
                        color = KcaColors.White,
                        fontSize = 14.dp,
                        bold = true,
                    )
                    TableText(
                        text = stringResource(R.string.expdtable_label_item2),
                        modifier = Modifier.weight(1f).height(20.dp),
                        color = KcaColors.White,
                        fontSize = 14.dp,
                        bold = true,
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    for (index in 0 until 4) {
                        TableText(
                            text = resourceValues[index].toString(),
                            modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                            color = valueColor,
                            fontSize = 14.dp,
                        )
                    }
                    RewardCount(reward1, Modifier.weight(1f))
                    RewardCount(reward2, Modifier.weight(1f))
                }

                // Success condition header + panel
                TableText(
                    text = stringResource(R.string.expdtable_label_condition),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(areaColor)
                        .padding(vertical = 2.dp),
                    color = KcaColors.White,
                    fontSize = 14.dp,
                    bold = true,
                )
                val info = remember(no) { KcaApiData.getExpeditionInfo(no) }
                if (info != null) {
                    ConditionPanel(info)
                }
            }
        }

        // `item_bottom`: the adapter paints this 2dp line in the area colour too.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(areaColor)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Row building blocks
// ---------------------------------------------------------------------------------------------

/** Centered single-line cell, the Compose equivalent of the item layout's TextViews. */
@Composable
private fun TableText(
    text: String,
    modifier: Modifier,
    color: Color,
    fontSize: Dp,
    bold: Boolean = false,
    background: Color = Color.Unspecified,
    singleLine: Boolean = true,
) {
    Box(
        modifier = if (background == Color.Unspecified) {
            modifier
        } else {
            modifier.background(background)
        },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize.asTextSize(),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
            overflow = if (singleLine) TextOverflow.Ellipsis else TextOverflow.Clip,
            textAlign = TextAlign.Center,
        )
    }
}

/** 20dp resource icon used in the expanded row header. */
@Composable
private fun RowScope.RewardHeaderIcon(iconRes: Int) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .weight(1f)
            .height(20.dp)
            .padding(vertical = 2.dp),
    )
}

/** Reward icon + count column of the expanded row; keeps the space when there is no reward. */
@Composable
private fun RowScope.RewardCount(reward: com.google.gson.JsonArray, modifier: Modifier) {
    val type = reward.get(0).asInt
    Row(modifier = modifier) {
        if (type > 0) {
            Image(
                painter = painterResource(
                    KcaUtils.getId("common_itemicons_id_$type", R.mipmap::class.java)
                ),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .padding(vertical = 2.dp),
            )
            Box(
                modifier = Modifier.weight(1f).padding(vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "x" + reward.get(1).asInt,
                    color = colorResource(R.color.black),
                    fontSize = 14.sp,
                )
            }
        } else {
            Spacer(modifier = Modifier.weight(1f).height(20.dp))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** Reward icon of the collapsed row; invisible (but sized) when there is no reward. */
@Composable
private fun RewardIcon(type: Int, modifier: Modifier) {
    if (type > 0) {
        Image(
            painter = painterResource(
                KcaUtils.getId("common_itemicons_id_$type", R.mipmap::class.java)
            ),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.padding(vertical = 2.dp),
        )
    } else {
        Spacer(modifier = modifier.padding(vertical = 2.dp))
    }
}

@Composable
private fun ConditionPanel(data: JsonObject) {
    val hasFlagLv = data.has("flag-lv")
    val hasFlagCond = data.has("flag-cond")
    val hasTotalLv = data.has("total-lv")
    val hasTotalCond = data.has("total-cond")
    val hasDrumShip = data.has("drum-ship")
    val hasDrumNum = data.has("drum-num")
    val hasDrumNumOptional = data.has("drum-num-optional")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
    ) {
        if (hasFlagLv || hasFlagCond) {
            ConditionRow(R.string.excheckview_flag_info) {
                if (hasFlagLv) {
                    ConditionValue(
                        stringResource(
                            R.string.excheckview_flag_lv_format,
                            data.get("flag-lv").asInt,
                        )
                    )
                }
                if (hasFlagCond) {
                    ConditionValue(
                        data.get("flag-cond").asString
                            .split("/")
                            .joinToString("/") { KcaApiData.getShipTypeAbbr(it.toInt()) }
                    )
                }
            }
        }

        ConditionRow(R.string.excheckview_total_info) {
            Column {
                Row {
                    ConditionValue(
                        stringResource(
                            R.string.excheckview_total_num_format,
                            data.get("total-num").asInt,
                        )
                    )
                    if (hasTotalLv) {
                        ConditionValue(
                            stringResource(
                                R.string.excheckview_total_lv_format,
                                data.get("total-lv").asInt,
                            )
                        )
                    }
                }
                if (hasTotalCond) {
                    data.get("total-cond").asString.split("/").forEach { condition ->
                        Text(
                            text = "- " + condition.split("|")
                                .joinToString(" ") { convertTotalCond(it) },
                            color = colorResource(R.color.black),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        if (hasDrumShip || hasDrumNum || hasDrumNumOptional) {
            ConditionRow(R.string.excheckview_drum_info) {
                if (hasDrumShip) {
                    ConditionValue(
                        stringResource(
                            R.string.excheckview_drum_ship_format,
                            data.get("drum-ship").asInt,
                        )
                    )
                }
                if (hasDrumNum) {
                    ConditionValue(
                        stringResource(
                            R.string.excheckview_drum_num_format,
                            data.get("drum-num").asInt,
                        )
                    )
                } else if (hasDrumNumOptional) {
                    ConditionValue(
                        stringResource(
                            R.string.excheckview_drum_num_format,
                            data.get("drum-num-optional").asInt,
                        )
                    )
                }
            }
        }

        TotalRow(data, "total-asw", R.string.excheckview_asw_info)
        TotalRow(data, "total-fp", R.string.excheckview_fp_info)
        TotalRow(data, "total-los", R.string.excheckview_los_info)
        TotalRow(data, "total-firepower", R.string.excheckview_firepower_info)
    }
}

@Composable
private fun TotalRow(data: JsonObject, key: String, labelRes: Int) {
    if (!data.has(key)) return
    ConditionRow(labelRes) {
        ConditionValue(
            stringResource(R.string.excheckview_total_format, data.get(key).asInt)
        )
    }
}

@Composable
private fun ConditionRow(labelRes: Int, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        // XML rows are plain horizontal LinearLayouts: the label aligns with the first line.
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.width(72.dp),
            color = colorResource(R.color.black),
            fontSize = 14.dp.asTextSize(),
            textAlign = TextAlign.Center,
        )
        content()
    }
}

@Composable
private fun ConditionValue(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(end = 4.dp),
        color = colorResource(R.color.black),
        fontSize = 14.sp,
    )
}

// ---------------------------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------------------------

private fun expeditionName(item: JsonObject): String {
    val name = item.getAsJsonObject("name")
    val locale = LocaleUtils.getResourceLocaleCode()
    return if (name.has(locale)) {
        name.get(locale).asString
    } else {
        name.get("jp").asString
    }
}

/** `CL/DD:2` style text for one `total-cond` entry. */
private fun convertTotalCond(condition: String): String {
    val shipCount = condition.split("-")
    val ships = shipCount[0].split(",")
        .joinToString("/") { KcaApiData.getShipTypeAbbr(it.toInt()) }
    return ships + ":" + shipCount[1]
}
