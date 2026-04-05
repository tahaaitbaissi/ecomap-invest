import dynamic from "next/dynamic";
import type { MapViewerProps } from "@/components/map/MapViewer";

const DynamicMap = dynamic<MapViewerProps>(() => import("./MapViewer"), {
  ssr: false,
  loading: () => (
    <div className="flex h-full w-full items-center justify-center bg-gray-900">
      <p className="animate-pulse text-sm tracking-widest text-gray-400">Loading map...</p>
    </div>
  ),
});

export default DynamicMap;
