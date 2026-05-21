import React, {
  useState,
  useRef,
  useEffect,
  useCallback,
  useMemo,
} from "react";
import "./PieMenu.css";

// ─── State machine ────────────────────────────────────────────────────────────
const S = {
  IDLE: "IDLE",
  MAIN_MENU: "MAIN_MENU",
  PEN_SUBMENU: "PEN_SUBMENU",
  COLOR_SUBMENU: "COLOR_SUBMENU",
  AI_SUBMENU: "AI_SUBMENU",
};

// ─── Math helpers ─────────────────────────────────────────────────────────────
const toRad = (d) => (d * Math.PI) / 180;
const polar = (angleDeg, r) => ({
  x: Math.cos(toRad(angleDeg)) * r,
  y: Math.sin(toRad(angleDeg)) * r,
});

function distributeOnArc(n, startDeg, endDeg, r) {
  if (n === 1) {
    const mid = (startDeg + endDeg) / 2;
    return [polar(mid, r)];
  }
  const step = (endDeg - startDeg) / (n - 1);
  return Array.from({ length: n }, (_, i) => polar(startDeg + step * i, r));
}

function svgArcPath(cx, cy, r, startDeg, endDeg) {
  const s = polar(startDeg, r);
  const e = polar(endDeg, r);
  const large = endDeg - startDeg > 180 ? 1 : 0;
  return `M ${cx + s.x} ${cy + s.y} A ${r} ${r} 0 ${large} 1 ${cx + e.x} ${cy + e.y}`;
}

function svgDonutArcPath(cx, cy, innerR, outerR, startDeg, endDeg) {
  const si = polar(startDeg, innerR);
  const ei = polar(endDeg, innerR);
  const so = polar(startDeg, outerR);
  const eo = polar(endDeg, outerR);
  const large = endDeg - startDeg > 180 ? 1 : 0;
  return [
    `M ${cx + so.x} ${cy + so.y}`,
    `A ${outerR} ${outerR} 0 ${large} 1 ${cx + eo.x} ${cy + eo.y}`,
    `L ${cx + ei.x} ${cy + ei.y}`,
    `A ${innerR} ${innerR} 0 ${large} 0 ${cx + si.x} ${cy + si.y}`,
    "Z",
  ].join(" ");
}

// ─── Data ─────────────────────────────────────────────────────────────────────
const PEN_TOOLS = [
  { id: "fountain", label: "Fountain pen", icon: PenFountainIcon },
  { id: "ballpoint", label: "Ballpoint", icon: PenBallpointIcon },
  { id: "marker", label: "Marker", icon: PenMarkerIcon },
  { id: "brush", label: "Brush", icon: PenBrushIcon },
  { id: "pencil", label: "Pencil", icon: PenPencilIcon },
];

const MAIN_ICONS = [
  { id: "palette", angleDeg: 210, label: "Color", next: S.COLOR_SUBMENU },
  { id: "pen",     angleDeg: 270, label: "Pen type", next: S.PEN_SUBMENU },
  { id: "ai",      angleDeg: 325, label: "AI", next: S.AI_SUBMENU },
];

const LONG_PRESS_MS = 500;
const MAIN_RADIUS = 40;
const PEN_ARC_RADIUS = 118;
const COLOR_INNER_R = 68;
const COLOR_OUTER_R = 102;
const CENTER_R = 60;
const SVG = 260; // viewport half-size → full: 520×520
const CX = SVG;
const CY = SVG;

// ─── SVG Icon components ──────────────────────────────────────────────────────
function PenFountainIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M17 3l4 4-9.5 9.5L7 21l-4-4 9.5-9.5Z" />
      <line x1="14" y1="6" x2="18" y2="10" />
    </svg>
  );
}
function PenBallpointIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M12 19l7-7 3 3-7 7-3-3Z" />
      <path d="M18 13l-1.5-7.5L2 2l3.5 14.5L13 18l5-5Z" />
      <circle cx="11" cy="11" r="2" />
    </svg>
  );
}
function PenMarkerIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <rect x="3" y="8" width="13" height="8" rx="2" />
      <path d="M16 11h3l2 1-2 1h-3" />
    </svg>
  );
}
function PenBrushIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M3 17a5 5 0 017-5l7-7 2 2-7 7a5 5 0 01-9 3Z" />
    </svg>
  );
}
function PenPencilIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M15.232 5.232l3.536 3.536M9 11l4-4 6 6-4 4L9 11Z" />
      <path d="M3 21l4.5-1.5L3 15l-1.5 4.5Z" />
    </svg>
  );
}
function PaletteIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="12" cy="12" r="10" />
      <circle cx="8.5" cy="9" r="1.5" fill="currentColor" stroke="none" />
      <circle cx="15.5" cy="9" r="1.5" fill="currentColor" stroke="none" />
      <circle cx="12" cy="15" r="1.5" fill="currentColor" stroke="none" />
    </svg>
  );
}
function PenTypeIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M12 2l3 7H9l3-7Z" />
      <line x1="12" y1="9" x2="12" y2="22" />
    </svg>
  );
}
function AIIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <path d="M12 2a2 2 0 012 2c0 .74-.4 1.39-1 1.73V7h2a7 7 0 017 7H4a7 7 0 017-7h2V5.73A2 2 0 0112 2Z" />
      <path d="M4 14v3a8 8 0 0016 0v-3" />
      <line x1="9" y1="19" x2="9" y2="21" />
      <line x1="15" y1="19" x2="15" y2="21" />
    </svg>
  );
}

const ICON_MAP = {
  palette: PaletteIcon,
  pen: PenTypeIcon,
  ai: AIIcon,
};

// ─── ColorRing subcomponent ───────────────────────────────────────────────────
function ColorRing({ cx, cy, innerR, outerR, hue, onHueChange }) {
  const handleClick = useCallback(
    (e) => {
      e.stopPropagation();
      const rect = e.currentTarget.getBoundingClientRect();
      const dx = e.clientX - (rect.left + rect.width / 2);
      const dy = e.clientY - (rect.top + rect.height / 2);
      const angleDeg = (Math.atan2(dy, dx) * 180) / Math.PI;
      onHueChange(((angleDeg % 360) + 360) % 360);
    },
    [onHueChange]
  );

  const selectorPos = useMemo(() => polar(hue, (innerR + outerR) / 2), [hue, innerR, outerR]);
  const size = outerR * 2 + 8;

  return (
    <div
      className="color-ring-wrap"
      style={{ width: size, height: size }}
      onClick={handleClick}
    >
      <div
        className="color-ring"
        style={{
          width: size,
          height: size,
          "--inner-r": `${innerR}px`,
          "--outer-r": `${outerR}px`,
        }}
      />
      {/* selector dot */}
      <div
        className="color-ring-dot"
        style={{
          transform: `translate(calc(-50% + ${selectorPos.x}px), calc(-50% + ${selectorPos.y}px))`,
          backgroundColor: `hsl(${hue},100%,50%)`,
        }}
      />
    </div>
  );
}

// ─── PenArc subcomponent ──────────────────────────────────────────────────────
function PenArc({ cx, cy, activePen, onSelectPen }) {
  const ARC_START = 200;
  const ARC_END = 340;
  const INNER = 88;
  const OUTER = 148;
  const positions = useMemo(
    () => distributeOnArc(PEN_TOOLS.length, ARC_START, ARC_END, PEN_ARC_RADIUS),
    []
  );
  const bgPath = svgDonutArcPath(cx, cy, INNER, OUTER, ARC_START, ARC_END);

  return (
    <g className="pen-arc">
      <path d={bgPath} className="pen-arc-bg" />
      {PEN_TOOLS.map((tool, i) => {
        const Icon = tool.icon;
        const { x, y } = positions[i];
        const active = activePen === tool.id;
        return (
          <foreignObject
            key={tool.id}
            x={cx + x - 20}
            y={cy + y - 20}
            width={40}
            height={40}
            className={`pen-icon-fo ${active ? "active" : ""}`}
            onClick={(e) => {
              e.stopPropagation();
              onSelectPen(tool.id);
            }}
            data-label={tool.label}
          >
            <div xmlns="http://www.w3.org/1999/xhtml" className="pen-icon-inner">
              <Icon />
            </div>
          </foreignObject>
        );
      })}
    </g>
  );
}

// ─── Main PieMenu component ───────────────────────────────────────────────────
export default function PieMenu({ children }) {
  const [state, setState] = useState(S.IDLE);
  const [pos, setPos] = useState({ x: 0, y: 0 });
  const [hue, setHue] = useState(200);
  const [activePen, setActivePen] = useState("ballpoint");

  const timerRef = useRef(null);
  const originRef = useRef({ x: 0, y: 0 });
  const containerRef = useRef(null);
  const menuRef = useRef(null);

  const isOpen = state !== S.IDLE;

  // ── Long-press detection ────────────────────────────────────────────────────
  const startPress = useCallback((clientX, clientY) => {
    originRef.current = { x: clientX, y: clientY };
    timerRef.current = setTimeout(() => {
      setPos({ x: clientX, y: clientY });
      setState(S.MAIN_MENU);
    }, LONG_PRESS_MS);
  }, []);

  const cancelPress = useCallback(() => {
    clearTimeout(timerRef.current);
  }, []);

  const onPointerDown = useCallback(
    (e) => {
      if (e.target.closest(".pie-menu-root")) return;
      if (e.button !== undefined && e.button !== 0) return;
      startPress(e.clientX, e.clientY);
    },
    [startPress]
  );

  const onPointerMove = useCallback(
    (e) => {
      const dx = e.clientX - originRef.current.x;
      const dy = e.clientY - originRef.current.y;
      if (Math.sqrt(dx * dx + dy * dy) > 8) cancelPress();
    },
    [cancelPress]
  );

  const onPointerUp = useCallback(() => cancelPress(), [cancelPress]);

  // ── Click-away to close ─────────────────────────────────────────────────────
  useEffect(() => {
    if (!isOpen) return;
    const handle = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setState(S.IDLE);
      }
    };
    document.addEventListener("pointerdown", handle, { capture: true });
    return () => document.removeEventListener("pointerdown", handle, { capture: true });
  }, [isOpen]);

  // ── Keyboard dismiss ────────────────────────────────────────────────────────
  useEffect(() => {
    const handle = (e) => {
      if (e.key === "Escape") setState(S.IDLE);
    };
    document.addEventListener("keydown", handle);
    return () => document.removeEventListener("keydown", handle);
  }, []);

  // ── Clamp menu position to viewport ────────────────────────────────────────
  const menuStyle = useMemo(() => {
    const HALF = SVG; // half of SVG viewport (260px)
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    const x = Math.min(Math.max(pos.x, HALF + 8), vw - HALF - 8);
    const y = Math.min(Math.max(pos.y, HALF + 8), vh - HALF - 8);
    return {
      left: x,
      top: y,
      transform: "translate(-50%, -50%)",
    };
  }, [pos]);

  // ── Transition helper ───────────────────────────────────────────────────────
  const goTo = useCallback((nextState) => {
    setState(nextState);
  }, []);

  const handleMainIcon = useCallback(
    (icon) => {
      goTo(icon.next);
    },
    [goTo]
  );

  // ── Render ──────────────────────────────────────────────────────────────────
  const aiActive = state === S.AI_SUBMENU;

  return (
    <div
      ref={containerRef}
      className="pie-canvas"
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
    >
      {children}

      {isOpen && (
        <div
          ref={menuRef}
          className={`pie-menu-root pie-state-${state.toLowerCase()}`}
          style={menuStyle}
          onPointerDown={(e) => e.stopPropagation()}
        >
          <svg
            width={SVG * 2}
            height={SVG * 2}
            viewBox={`0 0 ${SVG * 2} ${SVG * 2}`}
            className="pie-svg"
            onClick={(e) => e.stopPropagation()}
          >
            {/* ── Pen submenu arc ── */}
            {state === S.PEN_SUBMENU && (
              <PenArc
                cx={CX}
                cy={CY}
                activePen={activePen}
                onSelectPen={(id) => {
                  setActivePen(id);
                  setState(S.MAIN_MENU);
                }}
              />
            )}

            {/* ── Central dark circle ── */}
            <circle
              cx={CX}
              cy={CY}
              r={CENTER_R}
              className={`center-circle ${aiActive ? "center-ai" : ""}`}
            />

            {/* ── Main icons ── */}
            {MAIN_ICONS.map((icon) => {
              const { x, y } = polar(icon.angleDeg, MAIN_RADIUS);
              const Icon = ICON_MAP[icon.id];
              const active = state === icon.next;
              return (
                <foreignObject
                  key={icon.id}
                  x={CX + x - 18}
                  y={CY + y - 18}
                  width={36}
                  height={36}
                  className={`main-icon-fo ${active ? "active" : ""}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    if (active) {
                      setState(S.MAIN_MENU);
                    } else {
                      handleMainIcon(icon);
                    }
                  }}
                >
                  <div xmlns="http://www.w3.org/1999/xhtml" className="main-icon-inner">
                    <Icon />
                  </div>
                </foreignObject>
              );
            })}

            {/* ── AI label ── */}
            {aiActive && (
              <text
                x={CX}
                y={CY + CENTER_R + 22}
                textAnchor="middle"
                className="ai-label"
              >
                Assistente note
              </text>
            )}
          </svg>

          {/* ── 'T' text button (offset left, outside SVG) ── */}
          <button
            className="text-tool-btn"
            onClick={(e) => {
              e.stopPropagation();
            }}
            title="Text tool"
          >
            T
          </button>

          {/* ── Color ring (absolute-positioned over SVG) ── */}
          {state === S.COLOR_SUBMENU && (
            <div className="color-ring-portal">
              <ColorRing
                cx={CX}
                cy={CY}
                innerR={COLOR_INNER_R}
                outerR={COLOR_OUTER_R}
                hue={hue}
                onHueChange={setHue}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
}
