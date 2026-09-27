package org.isomorphisms.preferencegym;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class PreferenceGym extends Activity {
    private static final int EXPORT_REQUEST = 7;

    private static final String DEFAULT_TRAINER = "isomorphismes";
    private static final String[] KNOWN_TRAINERS = {
        "Jared",
        "Bill",
        "Steven",
        "Mxd",
        "Giuseppe",
        "isomorphismes"
    };

    private TrainingStore store;
    private String current_trainer;
    private TextInputEditText prompt;
    private TextInputEditText answer_a;
    private TextInputEditText answer_b;
    private MaterialButton prefer_a;
    private MaterialButton prefer_b;
    private LinearLayout better_responses;
    private TextView saved_count;
    private String preferred = "";

    @Override
    protected void onCreate(Bundle state) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(state);

        store = new TrainingStore(this);
        current_trainer = store.current_trainer();
        setContentView(build_screen());

        Intent incoming = getIntent();
        set_text(prompt, incoming.getStringExtra("prompt"));
        set_text(answer_a, incoming.getStringExtra("response_a"));
        set_text(answer_b, incoming.getStringExtra("response_b"));
        update_saved_count();
    }

    private View build_screen() {
        int pad = dp(16);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root, match_wrap());

        TextView title = new TextView(this);
        title.setText("Preference training");
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium);
        root.addView(title, match_wrap());

        TextInputLayout trainer_shell = new TextInputLayout(this);
        trainer_shell.setHint("Trainer");
        trainer_shell.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        trainer_shell.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);

        MaterialAutoCompleteTextView trainer = new MaterialAutoCompleteTextView(trainer_shell.getContext());
        trainer.setInputType(InputType.TYPE_NULL);
        trainer.setAdapter(new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            KNOWN_TRAINERS
        ));
        trainer.setText(current_trainer, false);
        trainer.setOnItemClickListener((parent, view, position, id) -> {
            current_trainer = KNOWN_TRAINERS[position];
            try {
                store.set_current_trainer(current_trainer);
                update_saved_count();
            } catch (IOException failure) {
                toast("Could not save trainer: " + failure.getMessage());
            }
        });
        trainer_shell.addView(trainer, match_wrap());
        root.addView(trainer_shell, match_wrap_with_bottom(dp(8)));

        saved_count = new TextView(this);
        saved_count.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        root.addView(saved_count, match_wrap_with_bottom(dp(18)));

        prompt = add_text_field(root, "Prompt", 4);

        TextView compare = section("Compare responses");
        root.addView(compare, match_wrap_with_top_bottom(dp(8), dp(8)));

        MaterialCardView card_a = candidate_card("A");
        answer_a = (TextInputEditText) card_a.getTag();
        root.addView(card_a, match_wrap_with_bottom(dp(12)));

        MaterialCardView card_b = candidate_card("B");
        answer_b = (TextInputEditText) card_b.getTag();
        root.addView(card_b, match_wrap_with_bottom(dp(12)));

        MaterialButtonToggleGroup choices = new MaterialButtonToggleGroup(this);
        choices.setSingleSelection(true);

        prefer_a = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        prefer_a.setId(View.generateViewId());
        prefer_a.setText("Prefer A");
        prefer_a.setOnClickListener(v -> preferred = "a");

        prefer_b = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        prefer_b.setId(View.generateViewId());
        prefer_b.setText("Prefer B");
        prefer_b.setOnClickListener(v -> preferred = "b");

        choices.addView(prefer_a, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        choices.addView(prefer_b, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(choices, match_wrap_with_bottom(dp(20)));

        root.addView(section("Better responses"), match_wrap_with_bottom(dp(4)));

        TextView better_note = new TextView(this);
        better_note.setText("Optional. Each authored response becomes a supervised target and is preferred to both A and B. Authored responses are not ranked against one another.");
        better_note.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        root.addView(better_note, match_wrap_with_bottom(dp(10)));

        better_responses = new LinearLayout(this);
        better_responses.setOrientation(LinearLayout.VERTICAL);
        root.addView(better_responses, match_wrap());
        add_better_response();

        MaterialButton add_better = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        add_better.setText("Add another better response");
        add_better.setOnClickListener(v -> add_better_response());
        root.addView(add_better, match_wrap_with_bottom(dp(18)));

        MaterialButton save = new MaterialButton(this);
        save.setText("Save preference");
        save.setOnClickListener(v -> save_record());
        root.addView(save, match_wrap_with_bottom(dp(10)));

        MaterialButton export = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        export.setText("Export training files");
        export.setOnClickListener(v -> begin_export());
        root.addView(export, match_wrap_with_bottom(dp(10)));

        TextView storage_note = new TextView(this);
        storage_note.setText("Training data is stored as ordinary UTF-8 files. The ZIP export is only a transport wrapper.");
        storage_note.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodySmall);
        root.addView(storage_note, match_wrap());

        return scroll;
    }

    private MaterialCardView candidate_card(String label) {
        MaterialCardView card = new MaterialCardView(this);
        card.setContentPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        card.addView(column, match_wrap());

        TextView heading = section("Candidate " + label);
        column.addView(heading, match_wrap_with_bottom(dp(6)));

        TextInputLayout shell = new TextInputLayout(this);
        shell.setHint("Response " + label);
        shell.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

        TextInputEditText edit = new TextInputEditText(shell.getContext());
        edit.setGravity(android.view.Gravity.TOP);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setMinLines(6);
        shell.addView(edit, match_wrap());
        column.addView(shell, match_wrap());

        card.setTag(edit);
        return card;
    }

    private TextInputEditText add_text_field(LinearLayout parent, String label, int lines) {
        TextInputLayout shell = new TextInputLayout(this);
        shell.setHint(label);
        shell.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

        TextInputEditText edit = new TextInputEditText(shell.getContext());
        edit.setGravity(android.view.Gravity.TOP);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setMinLines(lines);
        shell.addView(edit, match_wrap());

        parent.addView(shell, match_wrap_with_bottom(dp(14)));
        return edit;
    }

    private void add_better_response() {
        String label = candidate_label(2 + better_responses.getChildCount());

        TextInputLayout shell = new TextInputLayout(this);
        shell.setHint("Better response " + label);
        shell.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);

        TextInputEditText edit = new TextInputEditText(shell.getContext());
        edit.setGravity(android.view.Gravity.TOP);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setMinLines(5);
        shell.addView(edit, match_wrap());

        better_responses.addView(shell, match_wrap_with_bottom(dp(10)));
    }

    private void save_record() {
        String prompt_text = text(prompt);
        String a = text(answer_a);
        String b = text(answer_b);

        if (prompt_text.trim().isEmpty() || a.trim().isEmpty() || b.trim().isEmpty()) {
            toast("Prompt, A, and B are required.");
            return;
        }

        if (!preferred.equals("a") && !preferred.equals("b")) {
            toast("Choose A or B.");
            return;
        }

        List<String> authored = new ArrayList<>();
        for (int index = 0; index < better_responses.getChildCount(); index++) {
            TextInputLayout shell = (TextInputLayout) better_responses.getChildAt(index);
            TextInputEditText edit = (TextInputEditText) shell.getEditText();
            String value = edit == null ? "" : text(edit);
            if (!value.trim().isEmpty()) {
                authored.add(value);
            }
        }

        try {
            String record_id = store.save(
                current_trainer,
                prompt_text,
                a,
                b,
                preferred,
                authored,
                value_or_empty(getIntent().getStringExtra("model_a")),
                value_or_empty(getIntent().getStringExtra("model_b")),
                value_or_empty(getIntent().getStringExtra("source"))
            );

            clear_form();
            update_saved_count();
            toast("Saved " + record_id);
        } catch (IOException failure) {
            toast("Save failed: " + failure.getMessage());
        }
    }

    private void clear_form() {
        prompt.setText("");
        answer_a.setText("");
        answer_b.setText("");
        preferred = "";
        MaterialButtonToggleGroup group = (MaterialButtonToggleGroup) prefer_a.getParent();
        group.clearChecked();

        better_responses.removeAllViews();
        add_better_response();
    }

    private void begin_export() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_TITLE, "preference-training-" + current_trainer + ".zip");
        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(int request_code, int result_code, Intent data) {
        super.onActivityResult(request_code, result_code, data);
        if (request_code != EXPORT_REQUEST || result_code != RESULT_OK || data == null) {
            return;
        }

        Uri destination = data.getData();
        if (destination == null) {
            return;
        }

        try {
            int count = store.export_zip(destination);
            toast("Exported " + count + " records.");
        } catch (IOException failure) {
            toast("Export failed: " + failure.getMessage());
        }
    }

    private void update_saved_count() {
        saved_count.setText(store.record_count(current_trainer) + " saved for " + current_trainer);
    }

    private TextView section(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge);
        return view;
    }

    private static String text(TextInputEditText edit) {
        if (edit.getText() == null) {
            return "";
        }
        return edit.getText().toString();
    }

    private static void set_text(TextInputEditText edit, String value) {
        if (value != null && !value.isEmpty()) {
            edit.setText(value);
        }
    }

    private static String value_or_empty(String value) {
        return value == null ? "" : value;
    }

    private static String candidate_label(int index) {
        if (index >= 0 && index < 26) {
            return Character.toString((char) ('A' + index));
        }
        return "answer-" + (index + 1);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams match_wrap() {
        return new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams match_wrap_with_bottom(int bottom) {
        LinearLayout.LayoutParams result = match_wrap();
        result.bottomMargin = bottom;
        return result;
    }

    private LinearLayout.LayoutParams match_wrap_with_top_bottom(int top, int bottom) {
        LinearLayout.LayoutParams result = match_wrap();
        result.topMargin = top;
        result.bottomMargin = bottom;
        return result;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static final class TrainingStore {
        private final Context context;
        private final File root;

        TrainingStore(Context context) {
            this.context = context;
            this.root = new File(context.getFilesDir(), "preference-training");
        }

        String current_trainer() {
            File selected = new File(root, "current-trainer.txt");
            if (!selected.isFile()) {
                return DEFAULT_TRAINER;
            }
            try (FileInputStream input = new FileInputStream(selected)) {
                byte[] bytes = new byte[(int) selected.length()];
                int read = input.read(bytes);
                String value = new String(bytes, 0, Math.max(read, 0), StandardCharsets.UTF_8).trim();
                for (String known : KNOWN_TRAINERS) {
                    if (known.equals(value)) {
                        return value;
                    }
                }
            } catch (IOException ignored) {
            }
            return DEFAULT_TRAINER;
        }

        void set_current_trainer(String trainer) throws IOException {
            require_known_trainer(trainer);
            if (!root.isDirectory() && !root.mkdirs()) {
                throw new IOException("cannot create training directory");
            }
            write(new File(root, "current-trainer.txt"), trainer + "\n");
        }

        int record_count(String trainer) {
            File records = records_root(trainer);
            File[] children = records.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."));
            return children == null ? 0 : children.length;
        }

        String save(
            String trainer,
            String prompt,
            String answer_a,
            String answer_b,
            String preferred,
            List<String> authored,
            String model_a,
            String model_b,
            String source
        ) throws IOException {
            require_known_trainer(trainer);

            File records = records_root(trainer);
            if (!records.isDirectory() && !records.mkdirs()) {
                throw new IOException("cannot create records directory");
            }

            String base_id = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(new Date());
            String record_id = base_id;
            int suffix = 1;
            File final_dir = new File(records, record_id);
            while (final_dir.exists()) {
                record_id = base_id + "-" + suffix;
                suffix += 1;
                final_dir = new File(records, record_id);
            }

            File temp_dir = new File(records, "." + record_id + ".tmp");
            if (!temp_dir.mkdir()) {
                throw new IOException("cannot create temporary record");
            }

            try {
                write(new File(temp_dir, "trainer.txt"), trainer + "\n");
                write(new File(temp_dir, "prompt.txt"), prompt);

                File responses = new File(temp_dir, "responses");
                if (!responses.mkdir()) {
                    throw new IOException("cannot create responses directory");
                }
                write(new File(responses, "a.txt"), answer_a);
                write(new File(responses, "b.txt"), answer_b);

                StringBuilder preference = new StringBuilder();
                preference.append("preferred\trejected\trelation\n");
                if (preferred.equals("a")) {
                    preference.append("a\tb\tpairwise-choice\n");
                } else {
                    preference.append("b\ta\tpairwise-choice\n");
                }

                StringBuilder supervised = new StringBuilder();
                supervised.append("response\tauthored_by\n");

                for (int i = 0; i < authored.size(); i++) {
                    String label = candidate_label(i + 2).toLowerCase(Locale.ROOT);
                    write(new File(responses, label + ".txt"), authored.get(i));
                    preference.append(label).append("\ta\tauthored-better\n");
                    preference.append(label).append("\tb\tauthored-better\n");
                    supervised.append(label).append("\t").append(trainer).append("\n");
                }

                write(new File(temp_dir, "preference.tsv"), preference.toString());
                write(new File(temp_dir, "supervised.tsv"), supervised.toString());

                long created_at_ms = System.currentTimeMillis();
                String metadata =
                    "key\tvalue\n" +
                    "schema_version\t1\n" +
                    "created_at_ms\t" + created_at_ms + "\n" +
                    "trainer\t" + clean_cell(trainer) + "\n" +
                    "model_a\t" + clean_cell(model_a) + "\n" +
                    "model_b\t" + clean_cell(model_b) + "\n" +
                    "source\t" + clean_cell(source) + "\n" +
                    "authored_response_count\t" + authored.size() + "\n";
                write(new File(temp_dir, "metadata.tsv"), metadata);
                write(new File(temp_dir, "complete"), "");

                if (!temp_dir.renameTo(final_dir)) {
                    throw new IOException("cannot publish completed record");
                }

                try {
                    append_index(trainer, record_id, created_at_ms, preferred, authored.size());
                } catch (IOException ignored) {
                    // index.tsv is derived; the immutable record remains canonical.
                }
            } catch (IOException failure) {
                delete_tree(temp_dir);
                throw failure;
            }

            return record_id;
        }

        int export_zip(Uri destination) throws IOException {
            File users = new File(root, "users");
            File[] trainers = users.listFiles(File::isDirectory);
            int count = 0;

            OutputStream raw = context.getContentResolver().openOutputStream(destination);
            if (raw == null) {
                throw new IOException("cannot open export destination");
            }

            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(raw))) {
                if (trainers != null) {
                    for (File trainer : trainers) {
                        File records = new File(trainer, "records");
                        File[] record_dirs = records.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."));
                        if (record_dirs == null) {
                            continue;
                        }
                        for (File record : record_dirs) {
                            add_tree(zip, record, root);
                            count += 1;
                        }

                        File index = new File(trainer, "index.tsv");
                        if (index.isFile()) {
                            add_file(zip, index, root);
                        }
                    }
                }
            }

            return count;
        }

        private File records_root(String trainer) {
            return new File(new File(new File(root, "users"), trainer), "records");
        }

        private void append_index(
            String trainer,
            String record_id,
            long created_at_ms,
            String preferred,
            int authored_count
        ) throws IOException {
            File user_root = new File(new File(root, "users"), trainer);
            if (!user_root.isDirectory() && !user_root.mkdirs()) {
                throw new IOException("cannot create user directory");
            }

            File index = new File(user_root, "index.tsv");
            boolean new_file = !index.exists();
            try (FileOutputStream out = new FileOutputStream(index, true)) {
                if (new_file) {
                    out.write("record_id\tcreated_at_ms\ttrainer\tpairwise_winner\tauthored_response_count\n".getBytes(StandardCharsets.UTF_8));
                }
                String row =
                    record_id + "\t" +
                    created_at_ms + "\t" +
                    trainer + "\t" +
                    preferred + "\t" +
                    authored_count + "\n";
                out.write(row.getBytes(StandardCharsets.UTF_8));
            }
        }

        private void require_known_trainer(String trainer) throws IOException {
            for (String known : KNOWN_TRAINERS) {
                if (known.equals(trainer)) {
                    return;
                }
            }
            throw new IOException("unknown trainer");
        }

        private static String clean_cell(String value) {
            return value
                .replace('\t', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ');
        }

        private static void write(File file, String value) throws IOException {
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(value.getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
        }

        private static void add_tree(ZipOutputStream zip, File file, File relative_root) throws IOException {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        add_tree(zip, child, relative_root);
                    }
                }
                return;
            }
            add_file(zip, file, relative_root);
        }

        private static void add_file(ZipOutputStream zip, File file, File relative_root) throws IOException {
            String relative = relative_root.toURI().relativize(file.toURI()).getPath();
            zip.putNextEntry(new ZipEntry("preference-training/" + relative));
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    zip.write(buffer, 0, read);
                }
            }
            zip.closeEntry();
        }

        private static void delete_tree(File file) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        delete_tree(child);
                    }
                }
            }
            file.delete();
        }
    }
}
