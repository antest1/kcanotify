package com.antest1.kcanotify;

import static com.antest1.kcanotify.KcaUseStatConstant.OPEN_TOOL;
import static com.antest1.kcanotify.KcaUtils.sendUserAnalytics;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;


public class ToolsFragment extends Fragment {

    public ToolsFragment() {
        super(R.layout.fragment_tool_list);
    }

    MaterialCardView view_fleetlist, view_shiplist, view_equipment, view_droplog, view_reslog, view_akashi, view_expcalc, view_expdtable;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view_fleetlist = view.findViewById(R.id.action_fleetlist);
        view_shiplist = view.findViewById(R.id.action_shiplist);
        view_equipment = view.findViewById(R.id.action_equipment);
        view_droplog = view.findViewById(R.id.action_droplog);
        view_reslog = view.findViewById(R.id.action_reslog);
        view_akashi = view.findViewById(R.id.action_akashi);
        view_expcalc = view.findViewById(R.id.action_expcalc);
        view_expdtable = view.findViewById(R.id.action_expdtable);

        view_fleetlist.setOnClickListener(v -> {
            sendUserAnalytics(getActivity().getApplicationContext(), OPEN_TOOL.concat("FleetInfo"), null);
            Intent intent = new Intent(getActivity(), FleetInfoActivity.class);
            startActivity(intent);
        });

        view_shiplist.setOnClickListener(v -> {
            sendUserAnalytics(getActivity().getApplicationContext(), OPEN_TOOL.concat("ShipList"), null);
            Intent intent = new Intent(getActivity(), ShipInfoActivity.class);
            startActivity(intent);
        });

        view_equipment.setOnClickListener(v -> {
            sendUserAnalytics(getActivity().getApplicationContext(), OPEN_TOOL.concat("EquipList"), null);
            Intent intent = new Intent(getActivity(), EquipmentInfoActivity.class);
            startActivity(intent);
        });

        view_droplog.setOnClickListener(v -> {
            sendUserAnalytics(getActivity().getApplicationContext(), OPEN_TOOL.concat("DropLog"), null);
            Intent intent = new Intent(getActivity(), DropLogActivity.class);
            startActivity(intent);
        });

        view_reslog.setOnClickListener(v -> {
            sendUserAnalytics(getActivity().getApplicationContext(), OPEN_TOOL.concat("ResourceLog"), null);
            Intent intent = new Intent(getActivity(), ResourceLogActivity.class);
            startActivity(intent);
        });

        view_akashi.setOnClickListener(v -> {
            sendUserAnalytics(getActivity().getApplicationContext(), OPEN_TOOL.concat("AkashiList"), null);
            Intent intent = new Intent(getActivity(), AkashiActivity.class);
            startActivity(intent);
        });

        view_expcalc.setOnClickListener(v -> {
            sendUserAnalytics(getActivity().getApplicationContext(), OPEN_TOOL.concat("ExperienceCalc"), null);
            Intent intent = new Intent(getActivity(), ExpCalcActivity.class);
            startActivity(intent);
        });

        view_expdtable.setOnClickListener(v -> {
            sendUserAnalytics(getActivity().getApplicationContext(), OPEN_TOOL.concat("ExpeditionTable"), null);
            Intent intent = new Intent(getActivity(), ExpeditionTableActivity.class);
            startActivity(intent);
        });
    }
}
