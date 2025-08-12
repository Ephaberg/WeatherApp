# Weather Information App

A JavaFX desktop application that displays **current weather** and **short-term forecasts** for a selected location.  
Includes:

- **Current Weather**
- **Automatic Day/Night Theme**
- **Short-Term Forecast**
- **Unit Conversion** (°C ↔ °F, km/h ↔ mph)
- **Clean JavaFX UI with CSS styling**

---

## Features

### 🌤 Current Weather

- Displays temperature, humidity, wind speed, and conditions for the selected location.

### ⏳ Short-Term Forecast

- Shows upcoming weather (e.g., next few hours) in a **ListView**.

### 🌙 Automatic Day/Night Mode

- The application automatically switches between day and night themes based on the system time.
- Background changes **automatically** based on time (CSS themes for day/night).

### 🔄 Unit Conversion

- Allows users to switch between **°C** and **°F** for temperature and **km/h** and **mph** for wind speed.

---

## Usage

---

- Toggle between **Celsius ↔ Fahrenheit** for temperature.
- Toggle between **km/h ↔ mph** for wind speed.

## Technologies Used

---

- **Java 17+**
- **JavaFX 17+**
- **Maven** (for build & dependencies)
- **CSS** for styling
- **OpenWeatherMap API** (for real-time weather data)

---

## Setup Instructions

---

### 1️⃣ Prerequisites

- Install **Java 17+** and **Maven** on your system.
- Java 17 or later installed ([Download Java](https://adoptium.net/temurin/releases/))
- Maven installed ([Download Maven](https://maven.apache.org/))
- An **OpenWeatherMap API Key** ([Get OpenWeatherMap API](https://openweathermap.org/api))

---

### 2️⃣ Clone the Project

```bash
git clone https://github.com/yourusername/weatherapp.git
cd weatherapp

```

---

### 3️⃣ Install Dependencies

```bash
mvn clean install
mvn javafx:run

```

---

### 4️⃣ Run the Application

```bash
java -jar .\target\WeatherApp-1.0.jar

```

---

### 5️⃣ Project Structure

```bash
weatherapp/
│── src/
│   ├── main/java/com/weatherapp/
│   │   ├── WeatherApp.java         # Main application
│   │   ├── WeatherService.java     # API calls
│   │   ├── Main.java               # Launcher
│   │   ├── Utils.java              # Utility methods
│   │   ├── WeatherData.java        # Lightweight model for current weather and forecast entries
│   │   └── HistoryManager.java     # Recent searches
│── src/main/resources/styles/      # Styles
│   ├── glass.css                   # CSS for day/night mode
│── pom.xml                         # Maven configuration
│── README.md                       # Documentation
│── LICENSE                         # License
│── history.json
