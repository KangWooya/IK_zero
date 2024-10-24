package com.ik_zero;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class UploadActivity extends AppCompatActivity {

    private String selectedInstrument = null;
    private String sheetName;
    private ArrayList<String> uploadedFiles;
    private ArrayAdapter<String> adapter;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        // 전달된 악보명 가져오기
        sheetName = getIntent().getStringExtra("sheet_name");
        TextView sheetNameTextView = findViewById(R.id.sheet_name_text);
        sheetNameTextView.setText(sheetName);  // 악보명 표시

        uploadedFiles = new ArrayList<>();
        ListView uploadedFilesListView = findViewById(R.id.lv_uploaded_files);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, uploadedFiles);
        uploadedFilesListView.setAdapter(adapter);

        loadUploadedFiles();

        // PDF 업로드 버튼 클릭 리스너
        Button uploadSheetButton = findViewById(R.id.btn_upload_sheet);
        uploadSheetButton.setOnClickListener(v -> showInstrumentSelectionDialog());

        // ActivityResultLauncher 초기화
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri pdfUri = result.getData().getData();
                    if (pdfUri != null && selectedInstrument != null) {
                        String fileName = getFileNameFromUri(pdfUri);  // 파일 이름 가져오기
                        saveFileToFolder(sheetName, selectedInstrument, pdfUri);  // 파일을 악보 폴더에 저장
                        updateFileList(selectedInstrument, fileName);
                    }
                }
            }
        );
        // 리스트 항목을 길게 누르면 파일 삭제 다이얼로그 표시
        uploadedFilesListView.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedItem = uploadedFiles.get(position);
            showDeleteConfirmationDialog(position, selectedItem);
            return true;
        });
    }

    // 악기 선택 다이얼로그 표시
    private void showInstrumentSelectionDialog() {
        final String[] instruments = {"피아노", "드럼", "베이스", "기타"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("악기를 선택하세요")
                .setItems(instruments, (dialog, which) -> {
                    selectedInstrument = instruments[which];  // 선택된 악기 저장
                    openFilePicker();  // 파일 선택기로 이동
                })
                .setNegativeButton("취소", (dialog, which) -> dialog.dismiss())
                .show();
    }
    // 파일 선택기 열기
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);  // ActivityResultLauncher를 통해 파일 선택기 시작
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment(); // 경로에서 이름 추출
        }
        return result;
    }

    private void saveFileToFolder(String sheetName, String instrument, Uri fileUri) {
        try {
            // 악보 폴더 경로
            File sheetFolder = new File(getFilesDir(), sheetName);
            if (!sheetFolder.exists()) {
                sheetFolder.mkdir();
            }

            // 파일 이름 추출
            String fileName = getFileNameFromUri(fileUri);
            String fileName_with_inst = instrument + "_" + fileName;
            File newFile = new File(sheetFolder, fileName_with_inst);

            // 파일을 저장하는 스트림
            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            OutputStream outputStream = new FileOutputStream(newFile);

            // 파일 복사
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 스트림 닫기
            inputStream.close();
            outputStream.close();

            Toast.makeText(this, "파일이 " + sheetFolder.getName() + " 폴더에 저장되었습니다.", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "파일 저장 중 오류 발생", Toast.LENGTH_SHORT).show();
        }
    }


    // 파일 리스트를 악보 폴더에서 불러와 리스트에 표시
    private void loadUploadedFiles() {
        if (sheetName == null) {
            Toast.makeText(this, "sheetname null", Toast.LENGTH_SHORT).show();
            return;  // sheetName이 null이면 폴더를 불러오지 않음
        }
        File sheetFolder = new File(getFilesDir(), sheetName);
        if (sheetFolder.exists() && sheetFolder.isDirectory()) {
            File[] files = sheetFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    String[] parts = fileName.split("_",2);
                    if (parts.length == 2){
                        String instrument = parts[0];
                        String actualFileName = parts[1];
                        String displayString = "악기: " + instrument +"\n파일: " + actualFileName;
                        uploadedFiles.add(displayString);
                    }
                    else {
                        uploadedFiles.add("파일: " + fileName);
                    }

                }
                adapter.notifyDataSetChanged();  // 리스트 갱신
            }
        }
    }

    private void updateFileList(String instrument, String fileName) {
        String displayString = "악기: " + instrument + "\n파일: " + fileName;  // 줄바꿈 추가
        uploadedFiles.add(displayString);
        adapter.notifyDataSetChanged();  // 리스트 갱신
    }


    // 삭제 확인 다이얼로그
    private void showDeleteConfirmationDialog(int position, String selectedItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("파일 삭제");
        builder.setMessage("이 파일을 삭제하시겠습니까?");

        builder.setPositiveButton("삭제", (dialog, which) -> {
            // 파일 삭제 로직 추가
            deleteFileFromFolder(selectedItem);
            uploadedFiles.remove(position);  // 리스트에서 항목 제거
            adapter.notifyDataSetChanged();  // 리스트뷰 갱신
        });

        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // 선택된 파일을 삭제하는 메서드
    private void deleteFileFromFolder(String selectedItem) {
        // 파일명에서 악기명 및 파일명 추출
        String[] parts = selectedItem.split("\n파일: ");
        if (parts.length == 2) {
            String instrumentPart = parts[0].replace("악기: ", "");  // 악기명 추출
            String fileNamePart = parts[1];  // 파일명 추출

            // 파일명 형식을 instrument + "_" + fileName으로 조합
            String actualFileName = instrumentPart + "_" + fileNamePart;

            // 파일 경로 설정
            File sheetFolder = new File(getFilesDir(), sheetName);
            File fileToDelete = new File(sheetFolder, actualFileName);

            // 파일 삭제
            if (fileToDelete.exists()) {
                boolean deleted = fileToDelete.delete();
                if (deleted) {
                    Toast.makeText(this, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "파일 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
