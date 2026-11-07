// Spanish locale for the installed FormCreate designer.
// Translation of the upstream MIT-licensed locale; AI UI is disabled.
export default {
  name: 'es',
  form: {
    field: 'Campo',
    title: 'Título',
    info: 'Información',
    control: 'Condiciones',
    labelPosition: 'Posición de la etiqueta',
    labelStyle: 'Estilo de la etiqueta',
    labelSuffix: 'Sufijo de la etiqueta',
    size: 'Tamaño del formulario',
    event: 'Eventos del formulario',
    labelWidth: 'Ancho de la etiqueta',
    hideRequiredAsterisk: 'Ocultar el asterisco de los campos obligatorios',
    showMessage: 'Mostrar errores de validación',
    inlineMessage: 'Mostrar la validación en línea',
    submitBtn: 'Mostrar el botón de envío',
    resetBtn: 'Mostrar el botón de restablecimiento',
    appendChild: 'Insertar elemento secundario',
    formMode: 'Modo del formulario',
    formName: 'Nombre del formulario',
    componentMode: 'Componente',
    htmlMode: 'HTML',
    document: 'Documentación',
    controlDocument: 'Consulta los detalles de configuración en {doc}',
    onSubmit: 'Al enviar el formulario',
    onReset: 'Después de restablecer el formulario',
    onCreated: 'Después de inicializar el formulario',
    onMounted: 'Después de montar el formulario',
    onReload: 'Después de recargar las reglas del formulario',
    onChange: 'Cuando cambia el valor del componente',
    beforeSubmit: 'Cuando se supera la validación del formulario',
    beforeFetch: 'Antes de solicitar datos remotos'
  },
  warning: {
    name: 'Identificador único para consultar y modificar las reglas del componente.',
    field: 'Campo que vincula los datos al componente. Debe empezar por una letra.',
    fetch: 'Carga datos remotos y actualiza el componente con el resultado.',
    fetchQuery: 'Parámetros GET incluidos en la URL.',
    fetchData: 'Parámetros POST incluidos en el cuerpo de la solicitud.',
    fetchDataType: 'Tipo de datos del cuerpo de la solicitud.',
    beforeFetch:
      'Permite modificar los parámetros antes del envío. Devuelve false o una promesa rechazada para cancelar la solicitud.',
    fetchParse: 'Procesa la respuesta y la convierte a la estructura necesaria.',
    language: 'Gestiona los datos multilingües y el idioma del contenido.'
  },
  computed: {
    fieldUsed: '[{label}] se usa en una fórmula. Modifica primero la fórmula correspondiente.',
    fieldExist: 'El campo [{label}] ya existe',
    fieldEmpty: 'El campo es obligatorio',
    fieldChar: 'El campo debe empezar por una letra'
  },
  validate: {
    type: 'Tipo',
    typePlaceholder: 'Selecciona una opción',
    trigger: 'Activador',
    mode: 'Método de validación',
    modes: {
      required: 'obligatorio',
      pattern: 'patrón',
      validator: 'validador',
      min: 'mínimo',
      max: 'máximo',
      len: 'longitud',
      maxLen: 'longitud máxima',
      minLen: 'longitud mínima',
      uppercase: 'mayúsculas',
      lowercase: 'minúsculas',
      email: 'formato de correo',
      url: 'formato de URL',
      ip: 'dirección IP',
      phone: 'número de teléfono',
      positive: 'número positivo',
      negative: 'número negativo',
      integer: 'entero',
      number: 'número'
    },
    types: {
      string: 'Texto',
      boolean: 'Booleano',
      array: 'Múltiple',
      number: 'Número',
      integer: 'Entero',
      float: 'Decimal',
      object: 'Objeto',
      date: 'Fecha',
      url: 'URL',
      email: 'Correo electrónico'
    },
    message: 'Error',
    auto: 'Automático',
    autoRequired: 'Introduce {title}',
    autoMode: 'Introduce un valor válido para {title}',
    requiredPlaceholder: 'Introduce un valor',
    required: 'Campo obligatorio',
    rule: 'Validación'
  },
  tableOptions: {
    handle: 'Acciones',
    add: 'Añadir',
    empty1: 'Haz clic en la esquina inferior derecha',
    empty2: 'para añadir una columna',
    rmCol: 'Eliminar columna actual',
    rmRow: 'Eliminar fila actual',
    batchRmCol: 'Eliminar columnas',
    batchRmRow: 'Eliminar filas',
    batchSplit: 'Dividir celdas',
    batchMerge: 'Combinar celdas',
    splitRow: 'Dividir en filas',
    splitCol: 'Dividir en columnas',
    mergeBottom: 'Combinar hacia abajo',
    mergeRight: 'Combinar hacia la derecha',
    addTop: 'Añadir encima',
    addBottom: 'Añadir debajo',
    addLeft: 'Añadir columna a la izquierda',
    addRight: 'Añadir columna a la derecha',
    keyValue: 'clave-valor'
  },
  struct: {
    title: 'Editar',
    only: 'Solo se permite un elemento [{label}]',
    errorMsg: 'El contenido tiene errores de sintaxis',
    configured: 'Configurado'
  },
  event: {
    title: 'Editar',
    create: 'Crear',
    list: 'Lista',
    placeholder: 'Introduce el nombre del evento',
    saveMsg: 'Guarda primero el evento que estás editando',
    type: 'Tipo',
    info: 'Información',
    label: 'Campo',
    inject: {
      api: 'API del formulario actual',
      rule: 'Reglas del formulario actual',
      self: 'Regla de generación del componente',
      option: 'Configuración del formulario',
      args: 'Parámetros originales del evento'
    }
  },
  eventInfo: {
    blur: 'Al perder el foco',
    focus: 'Al recibir el foco',
    change: 'Cuando cambia el valor vinculado',
    input: 'Cuando cambia el valor',
    clear: 'Al pulsar el botón de borrar',
    close: 'Al cerrar el componente',
    click: 'Al hacer clic en el componente',
    add: 'Al añadir',
    delete: 'Al eliminar',
    visibleChange: 'Al mostrar u ocultar el desplegable',
    calendarChange: 'Cuando cambia la fecha seleccionada',
    panelChange: 'Cuando cambia el panel de fechas',
    open: 'Al abrir',
    opened: 'Al terminar la animación de apertura',
    closed: 'Al terminar la animación de cierre',
    openAutoFocus: 'Cuando el contenido recibe el foco',
    closeAutoFocus: 'Cuando el foco sale del contenido',
    submit: 'Al enviar el formulario',
    confirm: 'Al confirmar',
    validateFail: 'Cuando falla la validación del formulario',
    hook_load: 'Después de cargar las reglas del componente',
    hook_mounted: 'Después de montar el componente',
    hook_deleted: 'Después de eliminar las reglas del componente',
    hook_watch: 'Después de cambiar las reglas del componente',
    hook_value: 'Después de cambiar el valor del componente',
    hook_hidden: 'Después de cambiar la visibilidad del componente',
    hook_titleClick: 'Al hacer clic en la etiqueta del componente'
  },
  fetch: {
    title: 'Configurar datos',
    create: 'Crear datos',
    config: 'Solicitud',
    action: 'Acción',
    actionRequired: 'Introduce un enlace válido',
    placeholder: 'Introduce el nombre de la fuente de datos',
    method: 'Método',
    data: 'Datos adjuntos',
    dataType: 'Tipo de datos',
    headers: 'Cabeceras',
    query: 'Consultar',
    parse: 'Procesamiento',
    response: 'Datos devueltos por la API',
    onError: 'Al producirse un error',
    beforeFetch: 'Antes del envío',
    remote: 'Remoto',
    static: 'Estático',
    optionsType: {
      fetch: 'Obtener datos',
      struct: 'Estático'
    },
    preview: 'Ver datos de respuesta',
    test: 'Probar solicitud',
    previewEmpty: 'Pulsa «Probar solicitud» para ver la respuesta',
    requestFailed: 'Error en la solicitud'
  },
  style: {
    width: 'Ancho',
    height: 'Alto',
    color: 'Color',
    backgroundColor: 'Color de fondo',
    margin: 'Margen externo',
    padding: 'Relleno interno',
    borderRadius: 'Radio del borde',
    border: 'Borde',
    solid: 'Continuo',
    dashed: 'Discontinuo',
    dotted: 'Punteado',
    double: 'Doble',
    opacity: 'Opacidad',
    scale: 'Escala',
    minWidth: 'Ancho mínimo',
    minHeight: 'Alto mínimo',
    maxWidth: 'Ancho máximo',
    maxHeight: 'Alto máximo',
    overflow: {
      name: 'Desbordamiento',
      visible: 'Visible',
      hidden: 'Oculto',
      scroll: 'Desplazamiento',
      auto: 'Desplazamiento automático al desbordar'
    },
    shadow: {
      name: 'Sombra',
      x: 'Desplazamiento horizontal',
      y: 'Desplazamiento vertical',
      vague: 'Radio de desenfoque',
      extend: 'Radio de expansión',
      inset: 'Interior',
      external: 'Exterior',
      mode: 'Modo',
      classic: 'Clásico',
      flat: 'Plano',
      solid: 'Tridimensional'
    },
    font: {
      name: 'Fuente',
      size: 'Tamaño',
      family: 'Familia',
      align: 'Alineación',
      height: 'Interlineado',
      spacing: 'Espaciado entre letras',
      preview: 'Vista previa'
    },
    family: {
      'Microsoft YaHei': 'Microsoft YaHei',
      SimSun: 'SimSun',
      SimHei: 'SimHei',
      KaiTi: 'KaiTi',
      FangSong: 'FangSong',
      LiSu: 'LiSu'
    },
    position: {
      name: 'Posición',
      static: 'Estática',
      relative: 'Relativa',
      absolute: 'Absoluta',
      fixed: 'Fija',
      sticky: 'Adherente'
    },
    decoration: {
      name: 'Decoración',
      underline: 'Subrayado',
      'line-through': 'Tachado',
      overline: 'Línea superior'
    },
    weight: {
      '300': 'Fino',
      '400': 'Predeterminado',
      '500': 'Medio',
      '700': 'Negrita',
      name: 'Grosor de fuente'
    }
  },
  designer: {
    component: 'Componente',
    id: 'ID único',
    name: 'Identificador',
    type: 'Tipo',
    form: 'Formulario',
    json: 'Reglas',
    style: 'Estilo',
    rule: 'Básico',
    advanced: 'Avanzado',
    props: 'Propiedades',
    customProps: 'Propiedades personalizadas',
    validate: 'Validar',
    event: 'Evento',
    clearWarn: 'No se puede deshacer el vaciado. ¿Quieres continuar?',
    childEmpty: 'Pulsa el botón \\e789 de la esquina inferior derecha para añadir una columna',
    dragEmpty: 'Arrastra aquí los componentes de la lista de la izquierda',
    unload: '¿Quieres salir de esta página?',
    comList: 'Componente'
  },
  language: {
    name: 'Idioma',
    add: 'Añadir',
    batchRemove: 'Eliminar selección',
    select: 'Seleccionar idioma'
  },
  menu: {
    main: 'Básico',
    aide: 'Auxiliar',
    layout: 'Diseño',
    component: 'Componente',
    subform: 'Subformulario',
    tree: 'Estructura'
  },
  props: {
    success: 'Correcto',
    error: 'Error',
    disabled: 'deshabilitado',
    time: 'hora',
    size: 'Tamaño',
    email: 'correo electrónico',
    number: 'número',
    globalData: 'Datos globales',
    mobile: 'Móvil',
    pc: 'Ordenador',
    reactive: 'Reactivo',
    title: 'Título',
    content: 'Contenido',
    collection: 'Objeto',
    group: 'Grupo',
    custom: 'Personalizado',
    change: 'Cambio',
    blur: 'Pérdida de foco',
    preview: 'Vista previa',
    clear: 'Borrar',
    cancel: 'Cancelar',
    close: 'Cerrar',
    ok: 'Aceptar',
    save: 'Guardar',
    refresh: 'Actualizar',
    submit: 'Enviar',
    reset: 'Restablecer',
    copy: 'Copiar',
    import: 'Importar',
    delete: 'Eliminar',
    hide: 'Oculto',
    show: 'Mostrar',
    position: 'Posición',
    render: 'Renderizar',
    large: 'grande',
    default: 'predeterminado',
    small: 'pequeño',
    always: 'siempre',
    never: 'nunca',
    hover: 'al pasar el cursor',
    click: 'al hacer clic',
    button: 'botón',
    year: 'año',
    month: 'mes',
    date: 'fecha',
    dates: 'fechas',
    week: 'semana',
    datetime: 'fecha y hora',
    'datetime-local': 'fecha y hora',
    datetimerange: 'intervalo de fechas y horas',
    daterange: 'intervalo de fechas',
    monthrange: 'intervalo de meses',
    left: 'izquierda',
    right: 'derecha',
    top: 'arriba',
    text: 'texto',
    picture: 'imagen',
    'picture-card': 'tarjeta de imagen',
    center: 'centro',
    vertical: 'vertical',
    horizontal: 'horizontal',
    manage: 'Gestionar',
    key: 'clave',
    name: 'Nombre',
    value: 'valor',
    inputData: 'Valor predeterminado',
    append: 'Añadir al final',
    options: 'Opciones',
    option: 'Opción',
    callback: 'Función de retorno',
    style: 'Estilo',
    card: 'Tarjeta',
    _self: 'Ventana actual',
    _blank: 'Nueva ventana',
    _parent: 'Ventana superior',
    _top: 'Ventana principal'
  },
  com: {
    cascader: {
      name: 'Selector en cascada',
      event: {
        expandChange: 'Cuando cambia el nodo expandido',
        removeTag: 'Al eliminar una etiqueta en selección múltiple'
      },
      props: {
        props: 'Opciones',
        placeholder: 'Texto de ejemplo',
        disabled: 'Deshabilitado',
        clearable: 'Permitir borrar la selección',
        showAllLevels: 'Mostrar la ruta completa del valor seleccionado',
        collapseTags: 'Contraer etiquetas en selección múltiple',
        collapseTagsTooltip:
          'Mostrar todas las etiquetas al pasar el cursor sobre una etiqueta contraída',
        separator: 'Separador',
        filterable: 'Permitir buscar opciones',
        tagType: 'Tipo'
      },
      propsOpt: {
        multiple: 'Permitir selección múltiple',
        expandTrigger: 'Modo de expansión del submenú',
        checkStrictly: 'Seleccionar padres e hijos de forma independiente',
        emitPath: 'Devolver los valores de todos los niveles de la ruta seleccionada',
        value: 'Propiedad del objeto que contiene el valor de la opción',
        label: 'Propiedad del objeto que contiene la etiqueta',
        children: 'Propiedad del objeto que contiene las opciones secundarias',
        disabled: 'Propiedad del objeto que indica si la opción está deshabilitada',
        leaf: 'Propiedad del objeto que identifica los nodos hoja'
      }
    },
    checkbox: {
      name: 'Casilla de selección',
      props: {
        input: 'Permitir introducir un valor',
        type: 'Tipo',
        disabled: 'Deshabilitado',
        min: 'Número mínimo de selecciones',
        max: 'Número máximo de selecciones',
        textColor: 'Color de texto del botón activo',
        fill: 'Color de borde y fondo del botón activo'
      }
    },
    col: {
      name: 'Columna',
      props: {
        span: 'Número de columnas ocupadas',
        offset: 'Columnas de separación a la izquierda',
        push: 'Desplazar a la derecha por columnas',
        pull: 'Desplazar a la izquierda por columnas'
      }
    },
    colorPicker: {
      name: 'Selector de color',
      event: {
        activeChange: 'Cuando cambia el color del panel'
      },
      props: {
        disabled: 'Deshabilitado',
        showAlpha: 'Permitir seleccionar transparencia',
        colorFormat: 'Formato del color',
        predefine: 'Colores predefinidos'
      }
    },
    datePicker: {
      name: 'Fecha',
      props: {
        pickerOptions: 'Opciones del selector de fecha y hora',
        readonly: 'Solo lectura',
        disabled: 'Deshabilitado',
        type: 'Tipo',
        editable: 'Permitir escribir en el campo',
        clearable: 'Mostrar el botón de borrar',
        placeholder: 'Texto de ejemplo para una fecha',
        startPlaceholder: 'Texto de ejemplo de la fecha inicial',
        endPlaceholder: 'Texto de ejemplo de la fecha final',
        format: 'Formato mostrado en el campo',
        align: 'Alineación',
        rangeSeparator: 'Separador del intervalo',
        unlinkPanels: 'Desvincular los dos paneles de fecha'
      }
    },
    dateRange: {
      name: 'Intervalo de fechas'
    },
    timeRange: {
      name: 'Intervalo de horas'
    },
    elAlert: {
      name: 'Aviso',
      description: 'Descripción',
      props: {
        title: 'Título',
        type: 'Tipo',
        description: 'Texto complementario',
        closable: 'Permitir cerrar',
        center: 'Centrar el texto',
        closeText: 'Texto del botón de cierre',
        showIcon: 'Mostrar icono',
        effect: 'Seleccionar un tema'
      }
    },
    elButton: {
      name: 'Botón',
      props: {
        formCreateChild: 'Contenido',
        size: 'Tamaño',
        type: 'Tipo',
        plain: 'Botón de estilo sencillo',
        round: 'Usar esquinas redondeadas',
        circle: 'Usar botón circular',
        loading: 'Mostrar estado de carga',
        disabled: 'Deshabilitado'
      }
    },
    elCard: {
      name: 'Tarjeta',
      props: {
        header: 'Título',
        shadow: 'Cuándo mostrar la sombra'
      }
    },
    elCollapse: {
      name: 'Contraer',
      event: {
        change: 'Panel activo: texto en modo acordeón o lista en los demás modos'
      },
      props: {
        accordion: 'Activar modo acordeón'
      }
    },
    elCollapseItem: {
      name: 'Panel plegable',
      props: {
        title: 'Título del panel',
        name: 'Identificador',
        disabled: 'Deshabilitado'
      }
    },
    elDivider: {
      name: 'Separador',
      props: {
        formCreateChild: 'Configurar contenido',
        contentPosition: 'Posición del contenido'
      }
    },
    elTabPane: {
      name: 'Panel de pestaña',
      props: {
        label: 'Título',
        disabled: 'Deshabilitado',
        name: 'Identificador de la pestaña',
        lazy: 'Cargar el panel al activar su pestaña'
      }
    },
    elTabs: {
      name: 'Pestañas',
      event: {
        tabClick: 'Al seleccionar una pestaña',
        tabChange: 'Cuando cambia la pestaña activa',
        tabRemove: 'Al pulsar el botón de eliminar pestaña',
        tabAdd: 'Al pulsar el botón de nueva pestaña',
        edit: 'Después de añadir o eliminar una pestaña'
      },
      props: {
        type: 'Tipo',
        closable: 'Permitir cerrar la pestaña',
        tabPosition: 'Posición de las pestañas',
        stretch: 'Ajustar el ancho de las pestañas'
      }
    },
    elTag: {
      name: 'Etiqueta',
      props: {
        formCreateNative: 'Mostrar título',
        formCreateTitle: 'Título',
        formCreateChild: 'Contenido',
        type: 'Tipo',
        size: 'Tamaño de la etiqueta',
        effect: 'Tema de la etiqueta',
        closable: 'Permitir cerrar',
        disableTransitions: 'Desactivar la transición animada',
        hit: 'Mostrar borde',
        round: 'Usar forma redondeada',
        color: 'Color de fondo'
      }
    },
    elTransfer: {
      name: 'Selector de transferencia',
      event: {
        leftCheckChange: 'Al seleccionar o deseleccionar elementos de la lista izquierda',
        rightCheckChange: 'Al seleccionar o deseleccionar elementos de la lista derecha'
      },
      props: {
        filterable: 'Permitir búsqueda',
        filterPlaceholder: 'Texto de ejemplo de búsqueda',
        targetOrder: 'Orden de los elementos de la lista derecha',
        targetOrderInfo:
          'original conserva el orden de los datos; push añade al final; unshift añade al principio',
        titles: 'Título',
        buttonTexts: 'Contenido del botón',
        props: 'Alias de los campos de datos'
      }
    },
    elTreeSelect: {
      name: 'Selector de árbol',
      event: {
        removeTag: 'Al eliminar una etiqueta en selección múltiple'
      },
      props: {
        multiple: 'Permitir selección múltiple',
        disabled: 'Deshabilitado',
        clearable: 'Permitir borrar la selección',
        collapseTags: 'Mostrar las selecciones múltiples como texto',
        multipleLimit: 'Máximo de selecciones múltiples; 0 indica sin límite',
        placeholder: 'Texto de ejemplo',
        props: 'Opciones',
        renderAfterExpand: 'Cargar los nodos secundarios al expandir por primera vez',
        defaultExpandAll: 'Expandir todos los nodos inicialmente',
        expandOnClickNode: 'Expandir o contraer al pulsar el nodo',
        checkOnClickNode: 'Seleccionar al pulsar el nodo',
        nodeKey: 'Propiedad que identifica cada nodo de forma única en todo el árbol'
      }
    },
    elImage: {
      name: 'Imagen',
      props: {
        src: 'Ruta de la imagen'
      }
    },
    fcTitle: {
      name: 'Título',
      props: {
        title: 'Título',
        size: 'Tamaño',
        align: 'Alineación'
      }
    },
    fcEditor: {
      name: 'Editor',
      props: {
        disabled: 'Deshabilitado'
      }
    },
    fcRow: {
      name: 'Fila',
      props: {
        gutter: 'Separación entre columnas',
        type: 'Modo de diseño flex',
        justify: 'Distribución horizontal en flex',
        align: 'Distribución vertical en flex'
      }
    },
    fcTable: {
      name: 'Tabla',
      props: {
        border: 'Mostrar borde',
        borderColor: 'Color del borde',
        borderWidth: 'Ancho del borde'
      }
    },
    fcTableGrid: {
      name: 'Cuadrícula'
    },
    signaturePad: {
      name: 'Firma',
      info: 'Antes de eliminar la firma; devuelve false para impedirlo',
      props: {
        penColor: 'Color de la línea',
        disabled: 'Deshabilitado',
        beforeRemove: 'Función previa a la eliminación'
      }
    },
    group: {
      name: 'Subformulario',
      info: 'Antes de eliminar un elemento del subformulario; devuelve false para impedirlo',
      props: {
        type: 'Tipo',
        title: 'Título',
        disabled: 'Deshabilitado',
        expand: 'Elementos expandidos inicialmente',
        button: 'Mostrar botones de acciones',
        sortBtn: 'Mostrar botón de ordenación',
        min: 'Número mínimo de elementos',
        max: 'Número máximo de elementos',
        onBeforeRemove: 'Función previa a la eliminación'
      }
    },
    html: {
      name: 'HTML',
      props: {
        formCreateNative: 'Mostrar título',
        formCreateTitle: 'Título',
        formCreateChild: 'Contenido'
      }
    },
    input: {
      name: 'Campo de entrada',
      event: {
        change: 'Cuando cambia el valor y el campo pierde el foco o se pulsa Intro'
      },
      props: {
        type: 'Tipo',
        maxlength: 'Longitud máxima de entrada',
        minlength: 'Longitud mínima de entrada',
        placeholder: 'Texto de ejemplo',
        clearable: 'Mostrar el botón de borrar',
        disabled: 'Deshabilitado',
        readonly: 'Solo lectura'
      }
    },
    inputNumber: {
      name: 'Campo numérico',
      props: {
        precision: 'Número de decimales',
        min: 'Valor mínimo permitido',
        max: 'Valor máximo permitido',
        step: 'Incremento',
        stepStrictly: 'Permitir solo múltiplos del incremento',
        disabled: 'Deshabilitado',
        controls: 'Mostrar botones de incremento y disminución',
        controlsPosition: 'Posición de los botones de control',
        placeholder: 'Texto de ejemplo'
      }
    },
    password: {
      name: 'Contraseña',
      event: {
        change: 'Cuando cambia el valor y el campo pierde el foco o se pulsa Intro'
      },
      props: {
        disabled: 'Deshabilitado',
        readonly: 'Solo lectura',
        maxlength: 'Longitud máxima de entrada',
        minlength: 'Longitud mínima de entrada',
        placeholder: 'Texto de ejemplo',
        clearable: 'Mostrar el botón de borrar'
      }
    },
    radio: {
      name: 'Botones de opción',
      props: {
        input: 'Permitir introducir un valor',
        disabled: 'Deshabilitado',
        type: 'Tipo',
        textColor: 'Color de texto del botón activo',
        fill: 'Color de fondo y borde del botón activo'
      }
    },
    rate: {
      name: 'Valoración',
      props: {
        max: 'Puntuación máxima',
        disabled: 'Deshabilitado',
        allowHalf: 'Permitir medias puntuaciones',
        voidColor: 'Color del icono sin seleccionar',
        disabledVoidColor: 'Color del icono sin seleccionar en modo de solo lectura',
        voidIconClass: 'Clase del icono sin seleccionar',
        disabledVoidIconClass: 'Clase del icono sin seleccionar en modo de solo lectura',
        showScore: 'Mostrar puntuación actual',
        textColor: 'Color del texto auxiliar',
        scoreTemplate: 'Plantilla de puntuación'
      }
    },
    select: {
      name: 'Selector',
      event: {
        removeTag: 'Al eliminar una etiqueta en selección múltiple'
      },
      props: {
        multiple: 'Permitir selección múltiple',
        disabled: 'Deshabilitado',
        clearable: 'Permitir borrar la selección',
        collapseTags: 'Mostrar las selecciones múltiples como texto',
        multipleLimit: 'Máximo de selecciones múltiples; 0 indica sin límite',
        placeholder: 'Texto de ejemplo',
        filterable: 'Permitir búsqueda',
        allowCreate: 'Permitir crear nuevas opciones',
        noMatchText: 'Texto cuando no hay coincidencias',
        noDataText: 'Texto cuando no hay opciones',
        reserveKeyword: 'Conservar la búsqueda tras seleccionar una opción en selección múltiple',
        defaultFirstOption: 'Seleccionar la primera coincidencia al pulsar Intro',
        remote: 'Cargar opciones desde el servidor',
        remoteMethod: 'Método de búsqueda remota'
      }
    },
    slider: {
      name: 'Control deslizante',
      props: {
        min: 'Valor mínimo',
        max: 'Valor máximo',
        disabled: 'Deshabilitado',
        step: 'Incremento',
        showInput: 'Mostrar campo de entrada; solo sin selección de intervalo',
        showInputControls: 'Mostrar controles en el campo de entrada',
        showStops: 'Mostrar marcas de los incrementos',
        range: 'Seleccionar un intervalo',
        vertical: 'Orientación vertical',
        height: 'Alto del control; obligatorio en orientación vertical'
      }
    },
    space: {
      name: 'Espaciado',
      props: {
        height: 'Alto'
      }
    },
    subForm: {
      name: 'Grupo',
      props: {
        disabled: 'Deshabilitado',
        syncDisabled: 'Sincronizar el estado deshabilitado con el subformulario'
      }
    },
    switch: {
      name: 'Interruptor',
      props: {
        disabled: 'Deshabilitado',
        width: 'Ancho (px)',
        activeText: 'Texto del estado activado',
        inactiveText: 'Texto del estado desactivado',
        activeValue: 'Valor del estado activado',
        inactiveValue: 'Valor del estado desactivado',
        activeColor: 'Color de fondo al activar',
        inactiveColor: 'Color de fondo al desactivar'
      }
    },
    tableForm: {
      name: 'Formulario en tabla',
      props: {
        disabled: 'Deshabilitado',
        filterEmptyColumn: 'Excluir filas vacías',
        max: 'Número máximo de filas; 0 indica sin límite',
        min: 'Número mínimo de filas',
        addable: 'Permitir añadir',
        deletable: 'Permitir eliminar'
      }
    },
    tableFormColumn: {
      name: 'Columna del formulario en tabla',
      label: 'Columna del formulario en tabla',
      props: {
        align: 'Alineación',
        label: 'Título',
        width: 'Ancho',
        color: 'Color',
        required: 'Mostrar asteriscos de campos obligatorios'
      }
    },
    text: {
      name: 'Texto',
      props: {
        formCreateNative: 'Mostrar título',
        formCreateTitle: 'Título',
        formCreateChild: 'Contenido'
      }
    },
    textarea: {
      name: 'Área de texto',
      event: {
        change: 'Cuando cambia el valor y el campo pierde el foco o se pulsa Intro'
      },
      props: {
        disabled: 'Deshabilitado',
        readonly: 'Solo lectura',
        maxlength: 'Longitud máxima de entrada',
        minlength: 'Longitud mínima de entrada',
        showWordLimit: 'Mostrar recuento de caracteres',
        placeholder: 'Texto de ejemplo',
        rows: 'Número de filas de texto',
        autosize: 'Ajustar el alto automáticamente'
      }
    },
    timePicker: {
      name: 'Hora',
      props: {
        pickerOptions: 'Opciones del selector de fecha y hora',
        readonly: 'Solo lectura',
        disabled: 'Deshabilitado',
        editable: 'Permitir escribir en el campo',
        clearable: 'Mostrar el botón de borrar',
        placeholder: 'Texto de ejemplo para una fecha',
        startPlaceholder: 'Texto de ejemplo de la fecha inicial',
        endPlaceholder: 'Texto de ejemplo de la fecha inicial',
        isRange: 'Seleccionar un intervalo de horas',
        arrowControl: 'Usar flechas para seleccionar la hora',
        align: 'Alineación'
      }
    },
    tree: {
      name: 'Árbol',
      event: {
        nodeClick: 'Al hacer clic en un nodo',
        nodeContextmenu: 'Al hacer clic derecho en un nodo',
        checkChange: 'Al pulsar la casilla',
        check: 'Después de pulsar la casilla del nodo',
        currentChange: 'Cuando cambia el nodo seleccionado',
        nodeExpand: 'Al expandir un nodo',
        nodeCollapse: 'Al contraer un nodo',
        nodeDragStart: 'Al empezar a arrastrar un nodo',
        nodeDragEnter: 'Al arrastrar sobre otro nodo',
        nodeDragLeave: 'Al salir de un nodo durante el arrastre',
        nodeDragOver: 'Al arrastrar un nodo',
        nodeDragEnd: 'Al terminar el arrastre',
        nodeDrop: 'Al completar correctamente el arrastre'
      },
      props: {
        emptyText: 'Texto cuando no hay contenido',
        props: 'Opciones',
        renderAfterExpand: 'Cargar los nodos secundarios al expandir por primera vez',
        defaultExpandAll: 'Expandir todos los nodos inicialmente',
        expandOnClickNode:
          'Expandir o contraer al pulsar el nodo; si se desactiva, solo mediante la flecha.',
        checkOnClickNode: 'Seleccionar al pulsar el nodo',
        autoExpandParent: 'Expandir automáticamente el padre al expandir un nodo secundario',
        checkStrictly: 'Seleccionar las casillas de padres e hijos de forma independiente',
        accordion: 'Mantener expandido un solo nodo del mismo nivel',
        indent: 'Sangría horizontal entre niveles (px)',
        nodeKey: 'Propiedad que identifica cada nodo de forma única en todo el árbol'
      }
    },
    upload: {
      name: 'Cargar archivos',
      info: 'Tras la carga, asigna la URL a file.url o el resultado a file.value para enviarlo con el formulario.',
      event: {
        remove: 'Al quitar un archivo de la lista',
        preview: 'Al pulsar un archivo cargado',
        error: 'Cuando falla la carga',
        progress: 'Durante la carga de un archivo',
        exceed: 'Al superar el límite de archivos'
      },
      props: {
        listType: 'Tipo de carga',
        multiple: 'Permitir seleccionar varios archivos',
        action: 'URL de carga (obligatoria)',
        beforeUpload: 'Antes de cargar un archivo',
        onSuccess: 'Al completar la carga',
        beforeRemove: 'Antes de eliminar un archivo',
        headers: 'Cabeceras de la solicitud de carga',
        data: 'Parámetros adicionales de carga',
        name: 'Nombre del campo del archivo',
        withCredentials: 'Enviar credenciales de cookies',
        accept: 'Tipos de archivo admitidos',
        autoUpload: 'Cargar inmediatamente al seleccionar',
        disabled: 'Deshabilitado',
        limit: 'Número máximo de archivos'
      }
    }
  }
}
