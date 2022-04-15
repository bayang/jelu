// Create some re-useable definitions because
// many input types are identical in how
// we want to style them.
const textClassification = {
    label: 'label label-text font-semibold formkit-invalid:text-error capitalize',
    wrapper: `
    form-control
    `,
    input: 'input input-primary',
  }
  const boxClassification = {
    outer: ' mt-2',
    inner: 'self-center flex items-center content-center',
    input: 'checkbox checkbox-primary',
    fieldset: 'max-w-md border border-gray-400 rounded-md px-2 pb-1',
    legend: 'font-bold text-sm',
    wrapper: 'flex items-center content-center cursor-pointer mb-0',
    label: 'label label-text mt-1 mx-1 capitalize self-center'
  }
  const buttonClassification = {
    input: 'btn'
  }
  
  // export our definitions using our above
  // templates and declare one-offs and
  // overrides as needed.
  export default {
    // the global key will apply to all inputs
    global: {
      help: 'label label-text mt-0',
      message: 'text-error'
    },
    button: buttonClassification,
    color: {
      label: 'block mb-1 font-bold text-sm',
      input: 'w-16 h-8 appearance-none cursor-pointer border border-gray-300 rounded-md mb-2 p-1'
    },
    date: textClassification,
    'datetime-local': textClassification,
    checkbox: boxClassification,
    email: textClassification,
    file: {
      label: 'block mb-1 font-bold text-sm',
      inner: 'max-w-md cursor-pointer',
      input: 'text-gray-600 text-sm mb-1 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:bg-blue-500 file:text-white hover:file:bg-blue-600',
      noFiles: 'block text-gray-800 text-sm mb-1',
      fileItem: 'block flex text-gray-800 text-sm mb-1',
      removeFiles: 'ml-auto text-blue-500 text-sm'
    },
    month: textClassification,
    number: textClassification,
    password: textClassification,
    radio: {
      ...boxClassification,
      input: boxClassification.input.replace('rounded-sm', 'rounded-full'),
    },
    range: {
      inner: 'max-w-md',
      input: 'form-range appearance-none w-full h-2 p-0 bg-gray-200 rounded-full focus:outline-none focus:ring-0 focus:shadow-none'
    },
    search: textClassification,
    select: textClassification,
    submit: buttonClassification,
    tel: textClassification,
    text: textClassification,
    textarea: {
      ...textClassification,
      input: 'block w-full h-32 px-3 border-none text-base text-gray-700 placeholder-gray-400 focus:shadow-outline',
    },
    time: textClassification,
    url: textClassification,
    week: textClassification,
  }