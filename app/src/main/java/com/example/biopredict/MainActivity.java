package com.example.biopredict;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import java.nio.charset.StandardCharsets; // Importar para usar StandardCharsets

public class MainActivity extends AppCompatActivity {

    // Declaración de los elementos de la interfaz de usuario
    EditText inputFieldSleepDuration;
    EditText inputFieldQualityOfSleep;
    EditText inputFieldHeartRate;
    Button predictBtn;
    TextView resultTV;
    Interpreter interpreter;

    // Parámetros del escalador como variables finales
    private final float[] means = new float[3];
    private final float[] stds = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        try {
            // Cargar el modelo de TensorFlow Lite
            interpreter = new Interpreter(loadModelFile());
            loadScalerParams(); // Cargar parámetros de escalador
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Inicializar los campos de entrada y los botones de la interfaz de usuario
        inputFieldSleepDuration = findViewById(R.id.editTextSleepDuration);
        inputFieldQualityOfSleep = findViewById(R.id.editTextQualityOfSleep);
        inputFieldHeartRate = findViewById(R.id.editTextHeartRate);
        predictBtn = findViewById(R.id.button);
        resultTV = findViewById(R.id.textView);

        // Configurar el botón de predicción
        predictBtn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                // Obtener los valores de los campos de entrada
                String sleepDurationInput = inputFieldSleepDuration.getText().toString();
                String qualityOfSleepInput = inputFieldQualityOfSleep.getText().toString();
                String heartRateInput = inputFieldHeartRate.getText().toString();

                // Validar que los campos no estén vacíos
                if (sleepDurationInput.isEmpty() || qualityOfSleepInput.isEmpty() || heartRateInput.isEmpty()) {
                    resultTV.setText("Por favor, ingrese todos los valores.");
                    return;
                }

                // Convertir las entradas a valores de punto flotante
                float sleepDurationValue = Float.parseFloat(sleepDurationInput);
                float qualityOfSleepValue = Float.parseFloat(qualityOfSleepInput);
                float heartRateValue = Float.parseFloat(heartRateInput);

                // Escalar los valores de entrada
                float[] scaledInputs = scaleInputs(sleepDurationValue, qualityOfSleepValue, heartRateValue);

                // Crear un array para las entradas del modelo
                float[][] inputs = new float[1][3];
                inputs[0][0] = scaledInputs[0]; // Duración del sueño
                inputs[0][1] = scaledInputs[1]; // Calidad del sueño
                inputs[0][2] = scaledInputs[2]; // Frecuencia cardíaca

                // Realizar la inferencia con el modelo
                float result = doInference(inputs);

                // Mostrar el resultado en el TextView
                String stressLevel = result > 0.5 ? "Estrés alto" : "Estrés bajo";
                resultTV.setText("Valor de predicción: " + result + "\nNivel de estrés: " + stressLevel);
            }
        });

        // Configurar ajustes de diseño para los elementos en la pantalla
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Método para realizar la inferencia con el modelo de TensorFlow Lite
    public float doInference(float[][] input) {
        float[][] output = new float[1][1]; // Salida con un solo valor
        interpreter.run(input, output);
        return output[0][0]; // Retornar el valor predicho
    }

    // Método para cargar el modelo .tflite desde la carpeta assets
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor assetFileDescriptor = this.getAssets().openFd("modelo_estres.tflite");
        // Usar try-with-resources para asegurar que se cierran los recursos
        try (FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
             FileChannel fileChannel = fileInputStream.getChannel()) {
            long startOffset = assetFileDescriptor.getStartOffset();
            long length = assetFileDescriptor.getLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length);
        }
    }

    // Método para cargar los parámetros del escalador desde el archivo JSON
    private void loadScalerParams() {
        try (InputStream is = getAssets().open("scaler_params.json")) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            String json = new String(buffer, StandardCharsets.UTF_8); // Usar StandardCharsets
            JSONObject jsonObject = new JSONObject(json);
            // Obtener los parámetros de escalador
            for (int i = 0; i < means.length; i++) {
                means[i] = (float) jsonObject.getJSONArray("mean").getDouble(i);
                stds[i] = (float) jsonObject.getJSONArray("std").getDouble(i);
            }
        } catch (IOException | JSONException e) {
            Log.e("MainActivity", "Error al cargar los parámetros del escalador", e);
        }
    }

    // Método para escalar las entradas usando los parámetros cargados
    private float[] scaleInputs(float sleepDuration, float qualityOfSleep, float heartRate) {
        float[] scaledInputs = new float[3];
        scaledInputs[0] = (sleepDuration - means[0]) / stds[0]; // Escalar duración del sueño
        scaledInputs[1] = (qualityOfSleep - means[1]) / stds[1]; // Escalar calidad del sueño
        scaledInputs[2] = (heartRate - means[2]) / stds[2]; // Escalar frecuencia cardíaca
        return scaledInputs;
    }
}
