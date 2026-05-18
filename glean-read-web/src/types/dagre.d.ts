declare module "dagre" {
  const dagre: {
    graphlib: {
      Graph: new () => {
        setDefaultEdgeLabel: (factory: () => Record<string, unknown>) => void;
        setGraph: (label: Record<string, unknown>) => void;
        setNode: (id: string, value: Record<string, unknown>) => void;
        setEdge: (source: string, target: string) => void;
        node: (id: string) => { x: number; y: number; width: number; height: number };
      };
    };
    layout: (graph: unknown) => void;
  };

  export default dagre;
}
