package com.ik_zero;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class SheetManagerActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "SheetPrefs";
    private static final String SHEET_LIST_KEY = "SheetList";
    private ArrayList<String> sheetList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet_manager);

        Button btnMakeSheet = findViewById(R.id.btn_make_sheet);
        ListView uploadedSheetsListView = findViewById(R.id.lv_uploaded_sheets);

        // 악보 리스트 불러오기
        sheetList = loadSheetList();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sheetList);
        uploadedSheetsListView.setAdapter(adapter);

        // 악보 만들기 버튼 클릭 시 입력 창 띄우기
        btnMakeSheet.setOnClickListener(v -> showSheetNameInputDialog());

        // 리스트뷰 아이템 클릭 시 업로드 탭으로 이동
        uploadedSheetsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSheetName = sheetList.get(position);  // 클릭된 악보명 가져오기
            Intent intent = new Intent(SheetManagerActivity.this, UploadActivity.class);
            intent.putExtra("sheet_name", selectedSheetName);  // 악보명 전달
            startActivity(intent);  // 업로드 탭으로 이동
        });

        // 삭제 버튼 클릭 시 삭제 기능 추가
        uploadedSheetsListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showDeleteConfirmationDialog(position);
            return true;
        });
    }

    // 악보명 입력을 위한 AlertDialog
    private void showSheetNameInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("악보명 입력");

        final EditText input = new EditText(this);
        input.setHint("악보명을 입력하세요");
        builder.setView(input);

        // '확인' 버튼 클릭 리스너
        builder.setPositiveButton("확인", (dialog, which) -> {
            String sheetName = input.getText().toString().trim();
            if (!sheetName.isEmpty()) {
                createSheetFolder(sheetName);
                sheetList.add(sheetName);  // 입력된 악보명을 리스트에 추가
                adapter.notifyDataSetChanged();  // 리스트뷰 갱신
                saveSheetList();  // 리스트 저장
            }
        });

        // '취소' 버튼 클릭 리스너
        builder.setNegativeButton("취소", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // 삭제 확인 다이얼로그
    private void showDeleteConfirmationDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("삭제 확인");
        builder.setMessage("이 악보를 삭제하시겠습니까?");

        // '확인' 버튼
        builder.setPositiveButton("삭제", (dialog, which) -> {
            String sheetName = sheetList.get(position);  // 삭제할 악보명 가져오기
            File sheetFolder = new File(getFilesDir(), sheetName);  // 해당 악보 폴더 경로 가져오기

            // 폴더와 내부 파일 삭제
            if (deleteFolder(sheetFolder)) {
                // 삭제 성공 시 메시지 표시
                Toast.makeText(this, "악보 폴더가 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 삭제 실패 시 메시지 표시
                Toast.makeText(this, "악보 폴더 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
            }

            sheetList.remove(position);  // 리스트에서 삭제
            adapter.notifyDataSetChanged();  // 리스트뷰 갱신
            saveSheetList();  // 리스트 저장
        });

        // '취소' 버튼
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // 악보 리스트 저장
    private void saveSheetList() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(sheetList);
        editor.putString(SHEET_LIST_KEY, json);
        editor.apply();
    }

    // 악보 리스트 불러오기
    private ArrayList<String> loadSheetList() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(SHEET_LIST_KEY, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        ArrayList<String> list = gson.fromJson(json, type);
        return (list != null) ? list : new ArrayList<>();
    }

    private void createSheetFolder(String sheetName) {
        // 내부 저장소의 파일 경로 가져오기
        File sheetFolder = new File(getFilesDir(), sheetName);

        // 폴더가 존재하지 않으면 생성
        if (!sheetFolder.exists()) {
            sheetFolder.mkdir();
        }
    }
    // 폴더와 내부 파일들을 삭제하는 메서드
    private boolean deleteFolder(File folder) {
        boolean success = true;  // 삭제 성공 여부 플래그

        if (folder.isDirectory()) {
            // 폴더 내부의 모든 파일 및 폴더 삭제
            for (File child : folder.listFiles()) {
                // 자식 파일/폴더 삭제가 실패한 경우 플래그를 false로 설정
                if (!deleteFolder(child)) {
                    success = false;
                }
            }
        }

        // 폴더 또는 파일 삭제
        if (!folder.delete()) {
            success = false;  // 삭제 실패 시 플래그 false
        }

        return success;
    }
}
