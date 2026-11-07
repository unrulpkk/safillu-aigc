import es from 'element-plus/es/locale/lang/es'

// Element Plus 2.13.7 leaves many Spanish accessibility labels in English.
// Complete them locally, without modifying the dependency or falling back to English.
export default {
  ...es,
  el: {
    ...es.el,
    breadcrumb: { label: 'Ruta de navegación' },
    colorpicker: {
      ...es.el.colorpicker,
      clear: 'Borrar',
      defaultLabel: 'Selector de color',
      description: 'El color actual es {color}. Pulsa Intro para elegir otro.',
      alphaLabel: 'Elegir opacidad',
      alphaDescription: 'Opacidad {alpha}; color actual: {color}',
      hueLabel: 'Elegir tono',
      hueDescription: 'Tono {hue}; color actual: {color}',
      svLabel: 'Elegir saturación y brillo',
      svDescription: 'Saturación {saturation}, brillo {brightness}; color actual: {color}',
      predefineDescription: 'Seleccionar el color {value}'
    },
    datepicker: {
      ...es.el.datepicker,
      clear: 'Borrar',
      startDate: 'Fecha de inicio',
      dateTablePrompt: 'Usa las flechas y pulsa Intro para seleccionar el día del mes',
      monthTablePrompt: 'Usa las flechas y pulsa Intro para seleccionar el mes',
      yearTablePrompt: 'Usa las flechas y pulsa Intro para seleccionar el año',
      selectedDate: 'Fecha seleccionada',
      weeksFull: {
        sun: 'Domingo',
        mon: 'Lunes',
        tue: 'Martes',
        wed: 'Miércoles',
        thu: 'Jueves',
        fri: 'Viernes',
        sat: 'Sábado'
      }
    },
    inputNumber: { decrease: 'Disminuir valor', increase: 'Aumentar valor' },
    dropdown: { toggleDropdown: 'Abrir o cerrar menú' },
    pagination: {
      ...es.el.pagination,
      page: 'Página',
      prev: 'Ir a la página anterior',
      next: 'Ir a la página siguiente',
      currentPage: 'Página {pager}',
      prevPages: 'Retroceder {pager} páginas',
      nextPages: 'Avanzar {pager} páginas',
      deprecationWarning:
        'Se ha detectado una función obsoleta. Consulta la documentación de el-pagination.'
    },
    dialog: { close: 'Cerrar diálogo' },
    drawer: { close: 'Cerrar panel' },
    messagebox: { ...es.el.messagebox, title: 'Mensaje', close: 'Cerrar diálogo' },
    slider: {
      defaultLabel: 'Valor entre {min} y {max}',
      defaultRangeStartLabel: 'Seleccionar valor inicial',
      defaultRangeEndLabel: 'Seleccionar valor final'
    },
    table: {
      ...es.el.table,
      emptyText: 'Sin datos',
      clearFilter: 'Borrar filtro',
      selectAllLabel: 'Seleccionar todas las filas',
      selectRowLabel: 'Seleccionar esta fila',
      expandRowLabel: 'Expandir esta fila',
      collapseRowLabel: 'Contraer esta fila',
      sortLabel: 'Ordenar por {column}',
      filterLabel: 'Filtrar por {column}'
    },
    tag: { close: 'Cerrar etiqueta' },
    tour: { next: 'Siguiente', previous: 'Anterior', finish: 'Finalizar', close: 'Cerrar guía' },
    popconfirm: { confirmButtonText: 'Sí', cancelButtonText: 'No' },
    carousel: {
      leftArrow: 'Imagen anterior',
      rightArrow: 'Imagen siguiente',
      indicator: 'Ir a la imagen {index}'
    }
  }
}
