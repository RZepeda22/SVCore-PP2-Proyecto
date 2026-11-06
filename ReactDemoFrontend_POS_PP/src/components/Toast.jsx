import { createContext, useCallback, useContext, useRef, useState } from 'react';

const ToastContext = createContext(() => {});

export function useToast() {
    return useContext(ToastContext);
}

let nextId = 1;

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);
    const timers = useRef([]);

    const toast = useCallback((message, type = 'info') => {
        const id = nextId++;
        setToasts(list => [...list, { id, message, type, hiding: false }]);
        timers.current.push(setTimeout(() => {
            setToasts(list => list.map(t => t.id === id ? { ...t, hiding: true } : t));
        }, 4500));
        timers.current.push(setTimeout(() => {
            setToasts(list => list.filter(t => t.id !== id));
        }, 5000));
    }, []);

    return (
        <ToastContext.Provider value={toast}>
            {children}
            <div className="sv-toast-container" role="status" aria-live="polite">
                {toasts.map(t => (
                    <div key={t.id} className={'sv-toast sv-toast-' + t.type + (t.hiding ? ' sv-toast-hide' : '')}>
                        {t.message}
                    </div>
                ))}
            </div>
        </ToastContext.Provider>
    );
}
