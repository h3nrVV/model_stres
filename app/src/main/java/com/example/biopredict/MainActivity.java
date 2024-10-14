package com.example.biopredict;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    // Declaración de los elementos de la interfaz de usuario
    EditText inputFieldSleepDuration;
    EditText inputFieldQualityOfSleep; // Cambiado a Quality of Sleep
    EditText inputFieldHeartRate;
    Button predictBtn;
    TextView resultTV;
    Interpreter interpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        try {
            // Cargar el modelo de TensorFlow Lite
            interpreter = new Interpreter(loadModelFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Inicializar los campos de entrada y los botones de la interfaz de usuario
        inputFieldSleepDuration = findViewById(R.id.editTextSleepDuration);
        inputFieldQualityOfSleep = findViewById(R.id.editTextQualityOfSleep); // Cambiado a Quality of Sleep
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
                String qualityOfSleepInput = inputFieldQualityOfSleep.getText().toString(); // Cambiado a Quality of Sleep
                String heartRateInput = inputFieldHeartRate.getText().toString();

                // Validar que los campos no estén vacíos
                if (sleepDurationInput.isEmpty() || qualityOfSleepInput.isEmpty() || heartRateInput.isEmpty()) {
                    resultTV.setText("Por favor, ingrese todos los valores.");
                    return;
                }

                // Convertir las entradas a valores de punto flotante
                float sleepDurationValue = Float.parseFloat(sleepDurationInput);
                float qualityOfSleepValue = Float.parseFloat(qualityOfSleepInput); // Cambiado a Quality of Sleep
                float heartRateValue = Float.parseFloat(heartRateInput);

                // Crear un array para las entradas del modelo
                float[][] inputs = new float[1][3];
                inputs[0][0] = sleepDurationValue; // Duración del sueño
                inputs[0][1] = qualityOfSleepValue; // Calidad del sueño
                inputs[0][2] = heartRateValue; // Frecuencia cardíaca

                // Realizar la inferencia con el modelo
                float result = doInference(inputs);

                // Mostrar el resultado en el TextView
                String stressLevel = result > 0.5 ? "Estrés alto" : "Estrés bajo";
                resultTV.setText("Nivel de estrés: " + stressLevel);
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
        AssetFileDescriptor assetFileDescriptor = this.getAssets().openFd("mi_modelo_de_estres1.tflite");
        FileInputStream fileInputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = assetFileDescriptor.getStartOffset();
        long length = assetFileDescriptor.getLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length);
    }
}
