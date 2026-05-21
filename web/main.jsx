import React from "react";
import { createRoot } from "react-dom/client";
import PieMenu from "./PieMenu.jsx";

function App() {
  return (
    <PieMenu>
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          color: "rgba(255,255,255,0.15)",
          fontSize: 14,
          letterSpacing: "0.05em",
          pointerEvents: "none",
        }}
      >
        CANVAS AREA
      </div>
    </PieMenu>
  );
}

createRoot(document.getElementById("root")).render(<App />);
