package kr.ac.gachon.imutest;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;


import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] accelerationValues;
    private float[] magneticFieldValues;
    private long previousTimestamp;
    private float previousVelocityX, previousVelocityY, previousVelocityZ;
    private float previousPositionX, previousPositionY, previousPositionZ;

    private static final float GRAVITY_EARTH = SensorManager.GRAVITY_EARTH;
    private static final float NANOSECONDS_TO_SECONDS = 1.0f / 1000000000.0f; // 나노초를 초로 변환하는 상수
    private static final float MIN_MOVEMENT_THRESHOLD = 1.5f; // 이동 거리의 최소 임계값

    private TextView azimuthTextView;
    private TextView xMoveTextView;
    private TextView yMoveTextView;
    private TextView zMoveTextView;
    private TextView timeChangeTextView;

    private LineChart directionChart;
    private LineChart movementChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerationValues = new float[3];
        magneticFieldValues = new float[3];

        azimuthTextView = findViewById(R.id.azi);
        xMoveTextView = findViewById(R.id.X);
        yMoveTextView = findViewById(R.id.Y);
        zMoveTextView = findViewById(R.id.Z);
        timeChangeTextView = findViewById(R.id.CTime);

        directionChart = findViewById(R.id.directionChart);
        movementChart = findViewById(R.id.movementChart);
        setupChart(directionChart, "Direction");
        setupChart(movementChart, "Movement");

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        previousTimestamp = System.nanoTime();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerationValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldValues = event.values.clone();
        }

        // 가속도 및 자기장 데이터가 준비되면 방향 및 이동 거리 계산
        if (accelerationValues != null && magneticFieldValues != null) {
            float[] rotationMatrix = new float[9];
            boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerationValues, magneticFieldValues);
            if (success) {
                float[] orientationValues = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientationValues);

                // 방향 계산 (방위각)
                float azimuth = (float) Math.toDegrees(orientationValues[0]);

                // 이동 거리 계산
                float linearAccelerationX = accelerationValues[0] - GRAVITY_EARTH;
                float linearAccelerationY = accelerationValues[1] - GRAVITY_EARTH;
                float linearAccelerationZ = accelerationValues[2] - GRAVITY_EARTH;
                long currentTimestamp = System.nanoTime();
                float deltaTime = (currentTimestamp - previousTimestamp) * NANOSECONDS_TO_SECONDS;
                previousVelocityX += linearAccelerationX * deltaTime;
                previousVelocityY += linearAccelerationY * deltaTime;
                previousVelocityZ += linearAccelerationZ * deltaTime;
                previousPositionX += previousVelocityX * deltaTime;
                previousPositionY += previousVelocityY * deltaTime;
                previousPositionZ += previousVelocityZ * deltaTime;

                // 값의 오차가 일정 범위 이하인 경우 0으로 처리
                if (Math.abs(previousPositionX) < MIN_MOVEMENT_THRESHOLD) {
                    previousPositionX = 0.0f;
                }
                if (Math.abs(previousPositionY) < MIN_MOVEMENT_THRESHOLD) {
                    previousPositionY = 0.0f;
                }
                if (Math.abs(previousPositionZ) < MIN_MOVEMENT_THRESHOLD) {
                    previousPositionZ = 0.0f;
                }

                // 방향과 이동 거리를 사용하여 필요한 로직 수행
                // TODO: 방향과 이동 거리를 활용한 로직 작성

                azimuthTextView.setText(String.valueOf(azimuth));
                xMoveTextView.setText(String.valueOf(previousPositionX));
                yMoveTextView.setText(String.valueOf(previousPositionY));
                zMoveTextView.setText(String.valueOf(previousPositionZ));
                timeChangeTextView.setText(String.valueOf(deltaTime));

                // 방향 그래프 업데이트
                addEntry(directionChart, azimuth);

                // 이동 거리 그래프 업데이트
                float totalMovement = Math.abs(previousPositionX) + Math.abs(previousPositionY) + Math.abs(previousPositionZ);
                addEntry(movementChart, totalMovement);


                // 이전 시간 및 가속도 업데이트
                previousTimestamp = currentTimestamp;
                accelerationValues = null;
                magneticFieldValues = null;
            }
        }
    }

    private void setupChart(LineChart chart, String description) {
        // 그래프 기본 설정
        chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(true);
        Description desc = new Description();
        desc.setText(description);
        chart.setDescription(desc);
        chart.setTouchEnabled(false);

        // 데이터 초기화
        LineData data = new LineData();
        chart.setData(data);

        // 애니메이션 비활성화
        chart.animateXY(0, 0);

        // 범례 비활성화
        chart.getLegend().setEnabled(true);
    }

    private void addEntry(LineChart chart, float value) {
        LineData data = chart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            // 데이터 세트가 없으면 새로 생성
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            // 그래프에 엔트리 추가
            data.addEntry(new Entry(set.getEntryCount(), value), 0);
            data.notifyDataChanged();

            // 그래프 업데이트
            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(10);
            chart.moveViewToX(data.getEntryCount());
        }
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColors(ColorTemplate.getHoloBlue());
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        return set;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변경 이벤트 처리
    }
}
